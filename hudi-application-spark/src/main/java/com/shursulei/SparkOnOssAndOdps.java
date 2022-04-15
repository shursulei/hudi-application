package com.shursulei;
import org.apache.hudi.DataSourceReadOptions;
import org.apache.hudi.DataSourceWriteOptions;
import org.apache.hudi.QuickstartUtils;
import org.apache.hudi.config.HoodieWriteConfig;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.*;

import java.io.IOException;

import static org.apache.hudi.QuickstartUtils.getQuickstartWriteConfigs;
import static org.apache.hudi.common.table.HoodieTableConfig.PRECOMBINE_FIELD;
import static org.apache.hudi.config.HoodieWriteConfig.PRECOMBINE_FIELD_NAME;
import static org.apache.hudi.config.HoodieWriteConfig.TABLE_NAME;
import static org.apache.hudi.keygen.constant.KeyGeneratorOptions.PARTITIONPATH_FIELD_OPT_KEY;
import static org.apache.hudi.keygen.constant.KeyGeneratorOptions.RECORDKEY_FIELD_OPT_KEY;
import static org.apache.spark.sql.SaveMode.Overwrite;

/**
 * 将maxcompute的数据写入到oss上
 * @author hanfeng
 * @version 1.0
 * @date 2022/3/30 12:11
 */
public class SparkOnOssAndOdps {
  public static void main(String[] args) throws IOException {
    SparkSession spark = SparkSession
            .builder()
            .appName("SparkSQL-on-MaxCompute")
            .config("spark.sql.broadcastTimeout", 20 * 60)
            .config("spark.sql.crossJoin.enabled", true)
            .config("odps.exec.dynamic.partition.mode", "nonstrict")
            .getOrCreate();
    JavaSparkContext sparkContext = new JavaSparkContext(spark.sparkContext());
    String tableName = "mc_test_table";

    // 读 普通表
    Dataset<Row> rdf = spark.sql("select name, num,begintime from " + tableName);
    String basePath = "/bigdata/hudi_trips_cow";
    rdf.write().format("org.apache.hudi").
            options(getQuickstartWriteConfigs()).
            option(DataSourceWriteOptions.PRECOMBINE_FIELD_OPT_KEY(),"begintime").
            option(DataSourceWriteOptions.RECORDKEY_FIELD_OPT_KEY(), "num").
            option(TABLE_NAME, tableName).
            mode(Overwrite).
            save("oss://le-standard-bucket"+basePath);
  }
}
