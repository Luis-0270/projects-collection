package com.ludonghua.use.flinksql.SQL;

import com.ludonghua.common.utils.ExecutionEnvUtil;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

/**
 * Author Luis
 * DATE 2022-06-19 23:12
 */
public class KafkaToMySQL {
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

        // 3、注册SinkTable:mysql
        tableEnv.executeSql("create table sink_sensor (id string, ts bigint, vc int) with("
                + "'connector' = 'jdbc',"
                + "'url' = 'jdbc:mysql://hadoop102:3306/test',"
                + "'table-name' = 'sensor',"
                + "'username' = 'root',"
                + "'password' = '123123'"
                + ")");

//        // 4、执行查询Kafka数据
//        Table source_sensor = tableEnv.from("source_sensor");
//
//        // 5、写入MySQL
//        source_sensor.executeInsert("sink_sensor");

        tableEnv.executeSql("insert into sink_sensor select * from source_sensor");
    }
}
