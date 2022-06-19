package com.ludonghua.use.flinksql.SQL;

import com.ludonghua.common.utils.ExecutionEnvUtil;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * Author Luis
 * DATE 2022-06-19 22:20
 */
public class KafkaToKafka {
    public static void main(String[] args) throws Exception {
        // 1、获取流执行环境
        ParameterTool parameterTool = ExecutionEnvUtil.createParameterTool(args, "/application.properties");
        StreamExecutionEnvironment env = ExecutionEnvUtil.createEnv(parameterTool, false);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // 2、注册SourceTable
        tableEnv.executeSql("create table source_sensor (id string, ts bigint, vc int) with("
                + "'connector' = 'kafka',"
                + "'topic' = 'topic_source',"
                + "'properties.bootstrap.servers' = 'hadoop102:9092,hadoop103:9092,hadoop104:9092',"
                + "'properties.group.id' = 'test1',"
                + "'scan.startup.mode' = 'latest-offset',"
                + "'format' = 'csv'"
                + ")");


        // 3、注册SinkTable
        tableEnv.executeSql("create table sink_sensor(id string, ts bigint, vc int) with("
                + "'connector' = 'kafka',"
                + "'topic' = 'topic_sink',"
                + "'properties.bootstrap.servers' = 'hadoop102:9092,hadoop103:9092,hadoop104:9092',"
                + "'format' = 'json'"
                + ")");

        // 4、执行查询插入数据
        tableEnv.executeSql("insert into sink_sensor select * from source_sensor where id = 'ws_001'");
    }
}
