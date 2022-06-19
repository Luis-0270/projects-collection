package com.ludonghua.use.flinksql.processtime;

import com.ludonghua.common.utils.ExecutionEnvUtil;
import com.ludonghua.use.bean.WaterSensor;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import static org.apache.flink.table.api.Expressions.$;

/**
 * Author Luis
 * DATE 2022-06-19 23:49
 */
public class StreamToTable {
    public static void main(String[] args) throws Exception {
        // 1、获取流执行环境
        ParameterTool parameterTool = ExecutionEnvUtil.createParameterTool(args, "/application.properties");
        StreamExecutionEnvironment env = ExecutionEnvUtil.createEnv(parameterTool, false);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // 2、读取文本数据创建流并转换成JavaBean对象
        SingleOutputStreamOperator<WaterSensor> waterSensorDS = env.readTextFile("input-1.12.0/sensor.txt")
                .map(line -> {
                    String[] split = line.split(",");
                    return new WaterSensor(split[0],
                            Long.parseLong(split[1]),
                            Integer.parseInt(split[2]));
                });

        // 3、将流转换成表, 并指定处理时间
        Table table = tableEnv.fromDataStream(waterSensorDS,
                $("id"),
                $("ts"),
                $("vc"),
                $("pt").proctime());

        // 4、打印元数据信息
        table.printSchema();


    }
}
