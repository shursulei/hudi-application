package com.shursulei;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;

import static org.apache.hudi.QuickstartUtils.getQuickstartWriteConfigs;
import static org.apache.hudi.config.HoodieWriteConfig.TABLE_NAME;
import static org.apache.spark.sql.SaveMode.Overwrite;

/**
 * @author hanfeng
 * @version 1.0
 * @date 2022/3/31 21:07
 */
public class SparkOnOss {
  public static void main(String[] args) {
    //
    SparkSession spark = SparkSession
            .builder()
            .appName("SparkSQL-on-MaxCompute")
            .config("spark.sql.broadcastTimeout", 20 * 60)
            .config("spark.sql.crossJoin.enabled", true)
            .config("odps.exec.dynamic.partition.mode", "nonstrict")
            .getOrCreate();

      JavaSparkContext sparkContext = new JavaSparkContext(spark.sparkContext());
    // 读 普通表
    String tableName = "mc_test_table";
    Dataset<Row> rdf = spark.sql("select name, num from " + tableName);
    String basePath = "/bigdata/hudi_trips_cow";
    rdf.toDF().coalesce(1).write().mode(SaveMode.Overwrite).csv("oss://le-standard-bucket/bigdata/data3");
  }
}
