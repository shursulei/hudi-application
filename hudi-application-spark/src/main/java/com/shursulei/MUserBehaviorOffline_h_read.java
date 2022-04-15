package com.shursulei;

import org.apache.hudi.DataSourceWriteOptions;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.AnalysisException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.io.IOException;

import static org.apache.hudi.QuickstartUtils.getQuickstartWriteConfigs;
import static org.apache.hudi.config.HoodieWriteConfig.TABLE_NAME;
import static org.apache.spark.sql.SaveMode.Overwrite;

/**
 * 将maxcompute的数据写入到oss上
 * @author hanfeng
 * @version 1.0
 * @date 2022/3/30 12:11
 */
public class MUserBehaviorOffline_h_read {
  public static void main(String[] args) throws IOException {
    SparkSession spark = SparkSession
            .builder()
            .appName("SparkSQL-on-MaxCompute")
            .config("spark.sql.broadcastTimeout", 20 * 60)
            .config("spark.sql.crossJoin.enabled", true)
            .config("odps.exec.dynamic.partition.mode", "nonstrict")
            .getOrCreate();
    JavaSparkContext sparkContext = new JavaSparkContext(spark.sparkContext());
    String tableName = "m_user_behavior_offline_h";
    String tableName_v2 = "m_user_behavior_offline_h_v2";
    String basePath = "/bigdata/"+tableName;
    String basePath_v2 = "/bigdata/"+tableName_v2;
    Dataset<Row> df=spark.read()
            .format("hudi").option("as.of.instant","20220411133033770").load("oss://le-standard-bucket"+basePath);
    try {
      df.createTempView(tableName);
      Dataset<Row> df2=spark.sql("select * from "+tableName);
      df2.write().format("hudi").
              options(getQuickstartWriteConfigs()).
              option(DataSourceWriteOptions.PRECOMBINE_FIELD_OPT_KEY(),"ts").
              option(DataSourceWriteOptions.RECORDKEY_FIELD_OPT_KEY(), "user_id").
              option(TABLE_NAME, tableName_v2).
              mode(Overwrite).
              save("oss://le-standard-bucket"+basePath_v2);
    } catch (AnalysisException e) {
      e.printStackTrace();
    }
  }
}
