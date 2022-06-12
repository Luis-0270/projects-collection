package com.ludonghua.use.flinksql.test;

import com.ludonghua.use.bean.WaterSensor;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

import static org.apache.flink.table.api.Expressions.$;

/**
 * Author Luis
 * DATE 2022-06-12 22:59
 */
public class StreamingToTableTest {
    public static void main(String[] args) throws Exception {

        // 1、获取流执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

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

        // 6、将selectTable转换为流输出
        DataStream<Row> rowDataStream = tableEnv.toAppendStream(selectTable, Row.class);
        rowDataStream.print();

        // 7、执行
        env.execute();

    }
}
