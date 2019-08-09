package com.wugui.dataxweb.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 抽取日志记录表实体类(job_log)
 *
 * @author zhouhongfa@gz-yibo.com
 * @version v1.0
 * @since 2019-06-27
 */

@Data
@ApiModel
@TableName("job_log")
public class JobLog extends Model<JobLog> {

    /**
     *
     */
    @TableId
    @ApiModelProperty(value = "")
    private Long id;

    /**
     * 抽取任务，主键ID
     */
    @ApiModelProperty(value = "抽取任务，主键ID")
    private Long jobId;

    /**
     * 日志文件路径
     */
    @ApiModelProperty(value = "日志文件路径")
    private String logFilePath;

    /**
     *
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JSONField(format = "yyyy/MM/dd")
    @ApiModelProperty(value = "", hidden = true)
    private Date updateDate;

    /**
     *
     */
    @TableLogic
    @ApiModelProperty(value = "", hidden = true)
    private Integer status;

    /**
     *
     */
    @ApiModelProperty(value = "", hidden = true)
    private Integer createBy;

    /**
     *
     */
    @TableField(fill = FieldFill.INSERT)
    @JSONField(format = "yyyy/MM/dd")
    @ApiModelProperty(value = "", hidden = true)
    private Date createDate;

    /**
     *
     */
    @ApiModelProperty(value = "", hidden = true)
    private Integer updateBy;


    /**
     * 获取主键值
     *
     * @return 主键值
     */
    @Override
    protected Serializable pkVal() {
        return this.id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    public void setLogFilePath(String logFilePath) {
        this.logFilePath = logFilePath;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Integer createBy) {
        this.createBy = createBy;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Integer getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Integer updateBy) {
        this.updateBy = updateBy;
    }
}