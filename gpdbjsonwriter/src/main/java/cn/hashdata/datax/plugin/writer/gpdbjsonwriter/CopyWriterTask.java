package cn.hashdata.datax.plugin.writer.gpdbjsonwriter;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.datax.common.element.Column;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.plugin.TaskPluginCollector;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.rdbms.util.DBUtil;
import com.alibaba.datax.plugin.rdbms.util.DBUtilErrorCode;
import com.alibaba.datax.plugin.rdbms.util.DataBaseType;
import com.alibaba.datax.plugin.rdbms.writer.CommonRdbmsWriter;

public class CopyWriterTask extends CommonRdbmsWriter.Task {
	private static final char FIELD_DELIMITER = '|';
	private static final char NEWLINE = '\n';
	private static final char QUOTE = '"';
	private static final char ESCAPE = '\\';
	private static final Logger LOG = LoggerFactory.getLogger(CopyWriterTask.class);
	
	
	protected static class CopyWorker extends Thread {
		protected CopyManager mgr = null;
		protected String sql = null;
		protected InputStream in = null;
		protected Exception exce = null;

		public CopyWorker(BaseConnection conn, String sql, InputStream in) throws SQLException {
			this.sql = sql;
			this.in = in;
			mgr = new CopyManager(conn);
			this.setName(sql);
		}

		public Exception getCopyError() {
			return exce;
		}

		@Override
		public void run() {
			try {
				mgr.copyIn(sql, in);
			} catch (Exception e) {
				exce = e;
			} finally {
				try {
					in.close();
				} catch (Exception ignore) {
				}
			}
		}
	}

	public CopyWriterTask() {
		super(DataBaseType.PostgreSQL);
	}

	/**
	 * Any occurrence within the value of a QUOTE character or the ESCAPE
	 * character is preceded by the escape character.
	 */
	protected String escapeString(String data) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < data.length(); ++i) {
			char c = data.charAt(i);
			switch (c) {
			case 0x00:
				LOG.warn("字符串中发现非法字符 0x00，已经将其删除");
				continue;
			case QUOTE:
			case ESCAPE:
				sb.append(ESCAPE);
			}

			sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * Non-printable characters are inserted as '\nnn' (octal) and '\' as '\\'.
	 */
	protected String escapeBinary(byte[] data) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < data.length; ++i) {
			if (data[i] == '\\') {
				sb.append('\\');
				sb.append('\\');
			} else if (data[i] < 0x20 || data[i] > 0x7e) {
				byte b = data[i];
				char[] val = new char[3];
				val[2] = (char) ((b & 07) + '0');
				b >>= 3;
				val[1] = (char) ((b & 07) + '0');
				b >>= 3;
				val[0] = (char) ((b & 03) + '0');
				sb.append('\\');
				sb.append(val);
			} else {
				sb.append((char) (data[i]));
			}
		}

		return sb.toString();
	}

	protected byte[] serializeRecord(Record record) throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder();
		Column column;
		for (int i = 0; i < this.columnNumber; i++) {
			column = record.getColumn(i);
			String data = column.asString();
			if (data != null) {
				sb.append(QUOTE);
				sb.append(escapeString(data));
				sb.append(QUOTE);
			}
			if (i + 1 < this.columnNumber) {
				sb.append(FIELD_DELIMITER);
			}
		}
		sb.append(NEWLINE);
		return sb.toString().getBytes("UTF-8");
	}

	protected String getCopySql(String tableName, List<String> columnList, int segment_reject_limit) {
		StringBuilder sb = new StringBuilder().append("COPY ").append(tableName).append("(")
				.append(StringUtils.join(columnList, ","))
				.append(") FROM STDIN WITH DELIMITER '|' NULL '' CSV QUOTE '\"' ESCAPE E'\\\\'");

		if (segment_reject_limit >= 2) {
			sb.append(" LOG ERRORS SEGMENT REJECT LIMIT ").append(segment_reject_limit).append(";");
		} else {
			sb.append(";");
		}

		String sql = sb.toString();
		return sql;
	}

	@Override
	public void startWrite(RecordReceiver recordReceiver, Configuration writerSliceConfig,
			TaskPluginCollector taskPluginCollector) {
		Connection connection = DBUtil.getConnection(this.dataBaseType, this.jdbcUrl, username, password);
		DBUtil.dealWithSessionConfig(connection, writerSliceConfig, this.dataBaseType, BASIC_MESSAGE);

		int segment_reject_limit = writerSliceConfig.getInt("segment_reject_limit", 0);
		PipedOutputStream out = new PipedOutputStream();
		String sql = getCopySql(this.table, this.columns, segment_reject_limit);
		CopyWorker worker = null;
		Exception dataInError = null;

		this.resultSetMetaData = DBUtil.getColumnMetaData(connection, this.table, StringUtils.join(this.columns, ","));

		// Start a work thread to do copy work
		try {
			worker = new CopyWorker((BaseConnection) connection, sql, new PipedInputStream(out));
			worker.start();
		} catch (Exception e) {
			throw DataXException.asDataXException(DBUtilErrorCode.WRITE_DATA_ERROR, e);
		}

		try {
			Record record;
			while ((record = recordReceiver.getFromReader()) != null) {
				if (record.getColumnNumber() != this.columnNumber) {
					// 源头读取字段列数与目的表字段写入列数不相等，直接报错
					throw DataXException.asDataXException(DBUtilErrorCode.CONF_ERROR,
							String.format("列配置信息有错误. 因为您配置的任务中，源头读取字段数:%s 与 目的表要写入的字段数:%s 不相等. 请检查您的配置并作出修改.",
									record.getColumnNumber(), this.columnNumber));
				}

				byte[] data = serializeRecord(record);
				out.write(data);
			}

			out.flush();
		} catch (Exception e) {
			try {
				((BaseConnection) connection).cancelQuery();
			} catch (SQLException ignore) {
				// ignore if failed to cancel query
			}

			dataInError = e;
			throw DataXException.asDataXException(DBUtilErrorCode.WRITE_DATA_ERROR, e);
		} finally {
			try {
				out.close();
			} catch (Exception e) {
				// ignore if failed to close pipe
			}

			try {
				worker.join(0);
			} catch (Exception e) {
				// ignore if thread is interrupted
			}

			DBUtil.closeDBResources(null, null, connection);

			if (dataInError == null) {
				// no error happen from data input side,
				// check copy error
				Exception copyError = worker.getCopyError();

				if (copyError != null) {
					throw DataXException.asDataXException(DBUtilErrorCode.WRITE_DATA_ERROR, copyError);
				}
			} else {
				// ignore copy error if error happened on data input
				// side
			}
		}
	}
}
