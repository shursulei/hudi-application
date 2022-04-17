package com.shursulei.hudijava;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hudi.common.fs.FSUtils;
import org.apache.hudi.common.model.HoodieAvroPayload;
import org.apache.hudi.common.table.HoodieTableMetaClient;
import org.apache.hudi.config.HoodieCompactionConfig;
import org.apache.hudi.config.HoodieIndexConfig;
import org.apache.hudi.config.HoodieWriteConfig;
import org.apache.hudi.index.HoodieIndex;

import java.io.IOException;

public class JavaWrite {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: HoodieJavaWriteClientExample <tablePath> <tableName>");
            System.exit(1);
        }
        String tablePath = args[0];
        String tableName = args[1];
        Configuration hadoopConf = new Configuration();
        // initialize the table, if not done already
        Path path = new Path(tablePath);
        String tableType="";
        FileSystem fs = FSUtils.getFs(tablePath, hadoopConf);
        try {
            HoodieTableMetaClient.withPropertyBuilder()
                    .setTableType(tableType)
                    .setTableName(tableName)
                    .setPayloadClassName(HoodieAvroPayload.class.getName())
                    .initTable(hadoopConf, tablePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Create the write client to write some records in
        HoodieWriteConfig cfg = HoodieWriteConfig.newBuilder().withPath(tablePath)
                .withSchema(HoodieExampleDataGenerator.TRIP_EXAMPLE_SCHEMA).withParallelism(2, 2)
                .withDeleteParallelism(2).forTable(tableName)
                .withIndexConfig(HoodieIndexConfig.newBuilder().withIndexType(HoodieIndex.IndexType.INMEMORY).build())
                .withCompactionConfig(HoodieCompactionConfig.newBuilder().archiveCommitsWith(20, 30).build()).build();
//        HoodieJavaWriteClient<HoodieAvroPayload> client =
//                new HoodieJavaWriteClient<>(new HoodieJavaEngineContext(hadoopConf), cfg);
    }
}
