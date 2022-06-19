package com.ludonghua.use.flinksql.sink;

import com.ludonghua.common.utils.ExecutionEnvUtil;
import com.ludonghua.use.bean.WaterSensor;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.descriptors.*;
import org.apache.kafka.clients.producer.ProducerConfig;

import static org.apache.flink.table.api.Expressions.$;

/**
 * Author Luis
 * DATE 2022-06-19 21:04
 */
public class SinkES {
    public static void main(String[] args) throws Exception {
        // 1、获取流执行环境
        ParameterTool parameterTool = ExecutionEnvUtil.createParameterTool(args, "/application.properties");
        StreamExecutionEnvironment env = ExecutionEnvUtil.createEnv(parameterTool, false);

        // 2、读取端口数据创建流并转换为JavaBean
        SingleOutputStreamOperator<WaterSensor> waterSensorDS = env.socketTextStream("hadoop102", 9999)
                .map(data -> {
                    String[] split = data.split(",");
                    return new WaterSensor(split[0],
                            Long.parseLong(split[1]),
                            Integer.parseInt(split[2]));
                });

        // 3、创建表执行环境
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // 4、将流转换为动态表
        Table SensorTable = tableEnv.fromDataStream(waterSensorDS);

        // 5、使用TableAPI过滤出"ws_001"的数据
        Table selectTable = SensorTable.where($("id").isEqual("ws_001"))
                .select($("id"), $("ts"), $("vc"));

        // 6、将selectTable写入ES
        tableEnv.connect(new Elasticsearch()
                .index("sensor_sql")
                .documentType("_doc")
                .version("6")
                .host("hadoop102", 9200, "http")
                .bulkFlushMaxActions(1))
                .withSchema(new Schema()
                        .field("id", DataTypes.STRING())
                        .field("ts", DataTypes.BIGINT())
                        .field("vc", DataTypes.INT()))
                .withFormat(new Json())
                .inAppendMode()
                .createTemporaryTable("sensor");

//        tableEnv.from("sensor");             // Source
        selectTable.executeInsert("sensor"); // Sink

        // 7、执行
        env.execute();
    }
}
