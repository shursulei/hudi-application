package com.shursulei;

import org.apache.hudi.DataSourceWriteOptions;
import org.apache.spark.api.java.JavaSparkContext;
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
public class MUserBehaviorOffline_h {
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
//    System.out.println("system_args"+args[0]);
    // 读 普通表
    Dataset<Row> rdf =
        spark.sql(
            "select   user_id,\n"
                + "  gender,\n"
                + "  age,\n"
                + "  education,\n"
                + "  profession,\n"
                + "  income,\n"
                + "  marriage,\n"
                + "  store_id,\n"
                + "  store_name,\n"
                + "  province_id,\n"
                + "  province,\n"
                + "  city_id,\n"
                + "  city,\n"
                + "  district_id,\n"
                + "  district,\n"
                + "  lat,\n"
                + "  lng,\n"
                + "  brand_type_id,\n"
                + "  brand_type,\n"
                + "  unix_timestamp(start_time) as ts from lefit_produce."
                + tableName
                + " where pt=20220411 and hour=07");
    String basePath = "/bigdata/"+tableName;
    rdf.write().format("org.apache.hudi").
            options(getQuickstartWriteConfigs()).
            option(DataSourceWriteOptions.PRECOMBINE_FIELD_OPT_KEY(),"ts").
            option(DataSourceWriteOptions.RECORDKEY_FIELD_OPT_KEY(), "user_id").
            option(TABLE_NAME, tableName).
            mode(Overwrite).
            save("oss://le-standard-bucket"+basePath);
  }
}
