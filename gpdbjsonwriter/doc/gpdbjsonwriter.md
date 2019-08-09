# DataX GpdbJsonWriter


---


## 1 快速介绍

GpdbJsonWriter插件实现了写入数据到 Greenplum Database 主库目的表的功能。在底层实现上，GpdbJsonWriter通过JDBC连接远程 GPDB 数据库，并执行相应的 Copy TO 语句将数据写入 GPDB。

GpdbJsonWriter面向ETL开发工程师，他们使用GpdbJsonWriter从数仓导入数据到GPDB。同时 GpdbJsonWriter亦可以作为数据迁移工具为DBA等用户提供服务。


## 2 实现原理

GpdbJsonWriter通过 DataX 框架获取 Reader 生成的协议数据，根据你配置生成相应的SQL插入语句


* `copy to ...`

<br />

    注意：
    1. GpdbWriter和MysqlWriter不同，不支持配置writeMode参数。


## 3 功能说明

### 3.1 配置样例

* 这里使用一份从内存产生到 GpdbJsonWriter导入的数据。

```json
{
    "job": {
        "setting": {
            "speed": {
                "channel": 1
            }
        },
        "content": [
            {
                 "reader": {
                    "name": "mongodbjsonreader",
                    "parameter": {
                        "column" : [],
                        "address" : ["127.0.0.1:27017"],
                        "userName" : "",
                        "userPassword" : "",
                        "dbName" : "test",
                        "collectionName" : "restaurants"
                    }
                },
                "writer": {
                    "name": "gpdbjsonwriter",
                    "parameter": {
                        "username": "xx",
                        "password": "xx",
                        "segment_reject_limit": 0,
                        "column": [],
                        "preSql": [
                            "truncate table test"
                        ],
                        "connection": [
                            {
                                "jdbcUrl": "jdbc:postgresql://192.168.0.3:5432/tutorial",
                                "table": [
                                    "test"
                                ]
                            }
                        ]
                    }
                }
            }
        ]
    }
}

```


### 3.2 参数说明

* **jdbcUrl**

    * 描述：目的数据库的 JDBC 连接信息 ,jdbcUrl必须包含在connection配置单元中。

      注意：1、在一个数据库上只能配置一个值。
      2、jdbcUrl按照PostgreSQL官方规范，并可以填写连接附加参数信息。具体请参看PostgreSQL官方文档或者咨询对应 DBA。


  * 必选：是 <br />

  * 默认值：无 <br />

* **username**

  * 描述：目的数据库的用户名 <br />

  * 必选：是 <br />

  * 默认值：无 <br />

* **password**

  * 描述：目的数据库的密码 <br />

  * 必选：是 <br />

  * 默认值：无 <br />

* **segment\_reject\_limit**
  * 描述： 每个计算节点可接受的错误行数，0为不接受，或者大于1的正整数
  * 不选： 否
  * 默认值：0

* **table**

  * 描述：目的表的表名称。支持写入一个或者多个表。当配置为多张表时，必须确保所有表结构保持一致。

               注意：table 和 jdbcUrl 必须包含在 connection 配置单元中

  * 必选：是 <br />

  * 默认值：无 <br />

* **column**

  * 描述：目的表需要写入数据的字段,字段之间用英文逗号分隔。例如: "column": ["id","name","age"]。如果要依次写入全部列，使用*表示, 例如: "column": ["*"]

               注意：1、我们强烈不推荐你这样配置，因为当你目的表字段个数、类型等有改动时，你的任务可能运行不正确或者失败
                    2、此处 column 不能配置任何常量值

  * 必选：是 <br />

  * 默认值：否 <br />

* **preSql**

  * 描述：写入数据到目的表前，会先执行这里的标准语句。如果 Sql 中有你需要操作到的表名称，请使用 `@table` 表示，这样在实际执行 Sql 语句时，会对变量按照实际表名称进行替换。比如你的任务是要写入到目的端的100个同构分表(表名称为:datax_00,datax01, ... datax_98,datax_99)，并且你希望导入数据前，先对表中数据进行删除操作，那么你可以这样配置：`"preSql":["delete from @table"]`，效果是：在执行到每个表写入数据前，会先执行对应的 delete from 对应表名称 <br />

  * 必选：否 <br />

  * 默认值：无 <br />

* **postSql**

  * 描述：写入数据到目的表后，会执行这里的标准语句。（原理同 preSql ） <br />

  * 必选：否 <br />

  * 默认值：无 <br />

* **batchSize**

	* 描述：不支持此参数。<br />

	* 必选：否 <br />

	* 默认值：1024 <br />

### 3.3 类型转换

目前 GpdbWriter支持大部分 PostgreSQL类型，但也存在部分没有支持的情况，请注意检查你的类型。

下面列出 GpdbWriter针对 PostgreSQL类型转换列表:

| DataX 内部类型| PostgreSQL 数据类型    |
| -------- | -----  |
| Long     |bigint, bigserial, integer, smallint, serial |
| Double   |double precision, money, numeric, real |
| String   |varchar, char, text, bit|
| Date     |date, time, timestamp |
| Boolean  |bool|
| Bytes    |bytea|

## 4 性能报告


略


## FAQ

***

**Q: GpdbWriter 执行 postSql 语句报错，那么数据导入到目标数据库了吗?**

A: DataX 导入过程存在三块逻辑，pre 操作、导入操作、post 操作，其中任意一环报错，DataX 作业报错。由于 DataX 不能保证在同一个事务完成上述几个操作，因此有可能数据已经落入到目标端。

***

**Q: 按照上述说法，那么有部分脏数据导入数据库，如果影响到线上数据库怎么办?**

A: 目前有两种解法，第一种配置 pre 语句，该 sql 可以清理当天导入数据， DataX 每次导入时候可以把上次清理干净并导入完整数据。
第二种，向临时表导入数据，完成后再 rename 到线上表。

**Q: 为什么没有gpdbreader插件**

A: 使用postgresqlreader即可。

**Q: 为什么不用postgresqlwriter插件，而要开发一个新的插件gpdbjsonwriter？**

A: 对于GPDB 的Append optimized table 和 HAWQ，单条记录插入的效率非常低，而gpdbjsonwriter插件使用copy to语句而不是insert into语句，性能有巨大提升。


***


