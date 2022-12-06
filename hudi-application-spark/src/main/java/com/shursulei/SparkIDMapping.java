package com.shursulei;

import org.apache.hudi.DataSourceWriteOptions;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import static org.apache.hudi.QuickstartUtils.getQuickstartWriteConfigs;
import static org.apache.hudi.config.HoodieWriteConfig.TABLE_NAME;
import static org.apache.spark.sql.SaveMode.Overwrite;

public class SparkIDMapping {
    public static void main(String[] args) {
        SparkSession spark = SparkSession
                .builder()
                .appName("SparkSQL-on-MaxCompute")
                .config("spark.sql.broadcastTimeout", 20 * 60)
                .config("spark.sql.crossJoin.enabled", true)
                .config("odps.exec.dynamic.partition.mode", "nonstrict")
                .config("spark.hadoop.odps.project.name", "xxxxxxxxxxxxx")
                .config("spark.hadoop.odps.access.id", "xxxxxxxxxxxxxxxxx")
                .config("spark.hadoop.odps.access.key", "xxxxxxxxxxxxxxxxxxx")
                .config("spark.hadoop.odps.end.point", "http://service.cn.maxcompute.aliyun.com/api")
                .config("spark.sql.catalogImplementation", "hive")
                .config("spark.hadoop.fs.oss.accessKeyId","xxxxxxxxxxxxxxxxxxx")
                .config("spark.hadoop.fs.oss.accessKeySecret","xxxxxxxxxxxxxxxxxx")
                .config("spark.hadoop.fs.oss.endpoint","oss-cn-hangzhou-internal.aliyuncs.com")
                .config("spark.serializer","org.apache.spark.serializer.KryoSerializer")
                .getOrCreate();
        JavaSparkContext sparkContext = new JavaSparkContext(spark.sparkContext());
        String tableName = "spark_id_mapping";
//    System.out.println("system_args"+args[0]);
        // 读 普通表
        Dataset<Row> rdf =
                spark.sql("SELECT   user_id\n" +
                        "        ,CASE   WHEN android_id IS NOT NULL THEN android_id\n" +
                        "                ELSE device_id\n" +
                        "        END AS device_id\n" +
                        "        ,server_time\n" +
                        "FROM    o_sls_app_user_log_d\n" +
                        "WHERE   pt >= 20221101 and pt<20221201\n" +
                        "AND     device_id IS NOT NULL\n" +
                        "AND     user_id IS NOT NULL");
        String basePath = "/bigdata/"+tableName;
        rdf.write().format("org.apache.hudi").
                options(getQuickstartWriteConfigs()).
                option(DataSourceWriteOptions.PRECOMBINE_FIELD_OPT_KEY(),"server_time").
                option(DataSourceWriteOptions.RECORDKEY_FIELD_OPT_KEY(), "user_id,device_id").
                option(TABLE_NAME, tableName).
                mode(Overwrite).
                save("oss://le-standard-bucket"+basePath);
    }
    }

