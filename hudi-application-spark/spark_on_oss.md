spark on maxcompute and hudi on oss
版本
hudi 0.10.1
spark 版本2.4.5 spark-2.4.5-odps0.33.0
一、spark安装(使用版本，按照hudi官方的建议去做的)
参考阿里云的spark的版本安装
1、使用阿里云maxcompute->spark

过程文档
1、安装下载spark程序包spark-2.4.5-odps0.33.2
2、修改配置文件
```sql
spark.master = yarn-cluster
spark.driver.cores = 2
spark.driver.memory = 4g
#spark.executor.instances = 4
spark.dynamicAllocation.shuffleTracking.enabled = true
spark.dynamicAllocation.shuffleTracking.timeout = 20s
spark.dynamicAllocation.enabled = true
spark.dynamicAllocation.maxExecutors = 10
spark.dynamicAllocation.initialExecutors = 2
spark.executor.cores = 2
spark.executor.memory = 8g

spark.eventLog.enabled = true
spark.eventLog.overwrite = true
spark.eventLog.dir = odps://admin_task_project/cupidhistory/sparkhistory

spark.sql.catalogImplementation = hive
spark.sql.sources.default = hive
spark.hadoop.odps.project.name = xxxxxxx
spark.hadoop.odps.access.id = xxxxxxxxx
spark.hadoop.odps.access.key = xxxxxxxxxxxx
#spark.hadoop.odps.end.point = http://service-corp.odps.aliyun-inc.com/api
spark.hadoop.odps.end.point = http://service.cn-hangzhou.maxcompute.aliyun-inc.com/api
spark.hadoop.odps.runtime.end.point = http://service.cn.maxcompute.aliyun-inc.com/api
spark.hadoop.odps.spark.libs.public.enable=true
spark.hadoop.odps.spark.version=spark-2.4.5-odps0.34.0
spark.network.timeout=100000
spark.executor.heartbeatInterval=60
# spark 2.3.0请将spark.sql.catalogImplementation设置为odps，spark 2.4.5请将spark.sql.catalogImplementation设置为hive。
# 如下参数配置保持不变
spark.hadoop.odps.task.major.version = cupid_v2
spark.hadoop.odps.cupid.container.image.enable = true
spark.hadoop.odps.cupid.container.vm.engine.type = hyper
spark.hadoop.odps.cupid.webproxy.endpoint = http://service.cn.maxcompute.aliyun-inc.com/api
spark.hadoop.odps.moye.trackurl.host = http://jobview.odps.aliyun.com

```
3、编译生产jar包
4、执行jar包
```shell
bin/spark-submit --master yarn-cluster --class com.shursulei.SparkIDMapping  /home/admin/hudi-application-spark-1.0.0-shaded.jar
```
5、maxcompute 创建外部表

```sql
set odps.sql.hive.compatible = true;
set odps.sql.split.hive.bridge=true;
set odps.sql.jobconf.odps2=true;
set odps.sql.jobconf.odps2.enforce=true;
-- DROP TABLE spark_id_mapping;
-- 创建外部表
create external table spark_id_mapping
(
    user_id STRING ,
    device_id STRING  ,
    server_time STRING
)
ROW FORMAT SERDE 'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe'
with serdeproperties (
 'odps.properties.rolearn'='acs:ram::xxxxxxx:role/aliyunodpsdefaultrole'
)
STORED AS
INPUTFORMAT 'org.apache.hudi.hadoop.HoodieParquetInputFormat'
OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat'
location 'oss://oss-cn-hangzhou-internal.aliyuncs.com/le-standard-bucket/bigdata/spark_id_mapping';

set odps.sql.hive.compatible = true;
set odps.sql.split.hive.bridge=true;
set odps.sql.jobconf.odps2=true;
set odps.sql.jobconf.odps2.enforce=true;
select * from (
select 
user_id,COUNT(DISTINCT device_id) as total
from spark_id_mapping
GROUP by user_id) t where t.total>=2;

set odps.sql.hive.compatible = true;
set odps.sql.split.hive.bridge=true;
set odps.sql.jobconf.odps2=true;
set odps.sql.jobconf.odps2.enforce=true;
select t.user_id,t.device_id,t.server_time from spark_id_mapping 
t join (
select user_id from (
select 
user_id,COUNT(DISTINCT device_id) as total
from spark_id_mapping
GROUP by user_id) t where t.total>=2)
w
on t.user_id=w.user_id
ORDER by t.user_id,server_time
;

```