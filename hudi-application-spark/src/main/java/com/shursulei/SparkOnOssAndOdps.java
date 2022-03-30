package com.shursulei;

import com.aliyun.odps.Odps;
import com.aliyun.odps.cupid.CupidSession;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.spark.sql.types.StructField;

/**
 * @author hanfeng
 * @version 1.0
 * @date 2022/3/30 12:11
 */
public class SparkOnOssAndOdps {
  public static void main(String[] args) {
    SparkSession spark = SparkSession
            .builder()
            .appName("SparkSQL-on-MaxCompute")
            .config("spark.sql.broadcastTimeout", 20 * 60)
            .config("spark.sql.crossJoin.enabled", true)
            .config("odps.exec.dynamic.partition.mode", "nonstrict")
            .getOrCreate();
    JavaSparkContext sparkContext = new JavaSparkContext(spark.sparkContext());


    String tableName = "mc_test_table";
    String tableNameCopy = "mc_test_table_copy";
    String ptTableName = "mc_test_pt_table";


    spark.sql("DROP TABLE IF EXISTS " + tableName);
    spark.sql("DROP TABLE IF EXISTS " + tableNameCopy);
    spark.sql("DROP TABLE IF EXISTS " + ptTableName);

    spark.sql("CREATE TABLE " + tableName + " (name STRING, num BIGINT)");
    spark.sql("CREATE TABLE " + ptTableName + " (name STRING, num BIGINT) PARTITIONED BY (pt1 STRING, pt2 STRING)");

    List<Integer> data = new ArrayList<Integer>();
    for (int i = 0; i < 100; i++) {
      data.add(i);
    }

    JavaRDD<Row> dfRDD = sparkContext.parallelize(data, 2).map(new Function<Integer, Row>() {
      public Row call(Integer i) {
        return RowFactory.create(
                "name-" + i.toString(),
                Long.valueOf(i));
      }
    });

    JavaRDD<Row> ptDfRDD = sparkContext.parallelize(data, 2).map(new Function<Integer, Row>() {
      public Row call(Integer i) {
        return RowFactory.create(
                "name-" + i.toString(),
                Long.valueOf(i),
                "2018",
                "0601");
      }
    });

    List<StructField> structFilelds = new ArrayList<StructField>();
    structFilelds.add(DataTypes.createStructField("name", DataTypes.StringType, true));
    structFilelds.add(DataTypes.createStructField("num", DataTypes.LongType, true));
    Dataset<Row> df = spark.createDataFrame(dfRDD, DataTypes.createStructType(structFilelds));

    structFilelds.add(DataTypes.createStructField("pt1", DataTypes.StringType, true));
    structFilelds.add(DataTypes.createStructField("pt2", DataTypes.StringType, true));
    Dataset<Row> ptDf = spark.createDataFrame(ptDfRDD, DataTypes.createStructType(structFilelds));

    // 写 普通表
    df.write().insertInto(tableName); // insertInto语义
    df.write().mode("overwrite").insertInto(tableName);// insertOverwrite语义

    // 读 普通表
    Dataset<Row> rdf = spark.sql("select name, num from " + tableName);
    System.out.println("rdf count: " + rdf.count());
    rdf.printSchema();

    //create table as select
    spark.sql("CREATE TABLE " + tableNameCopy + " AS SELECT name, num FROM " + tableName);
    spark.sql("SELECT * FROM " + tableNameCopy).show();

    // 写 分区表
    // DataFrameWriter 无法指定分区写入 需要通过临时表再用SQL写入特定分区
    df.registerTempTable(ptTableName + "_tmp_view");
    spark.sql("insert into table " + ptTableName + " partition (pt1='2018', pt2='0601') select * from " + ptTableName + "_tmp_view");
    spark.sql("insert overwrite table " + ptTableName + " partition (pt1='2018', pt2='0601') select * from " + ptTableName + "_tmp_view");

    ptDf.write().insertInto(ptTableName);// 动态分区 insertInto语义
    ptDf.write().mode("overwrite").insertInto(ptTableName); // 动态分区 insertOverwrite语义

    // 读 分区表
    Dataset<Row> rptdf = spark.sql("select name, num, pt1, pt2 from " + ptTableName + " where pt1 = '2018' and pt2 = '0601'");
    System.out.println("rptdf count: " + rptdf.count());
    rptdf.printSchema();

    Odps odps = CupidSession.get().odps();
    System.out.println(odps.tables().get(ptTableName).getPartitions().size());
    System.out.println(odps.tables().get(ptTableName).getPartitions().get(0).getPartitionSpec());
  }
}
