package com.ludonghua.use.flinksql.test;

import com.ludonghua.common.utils.ExecutionEnvUtil;
import com.ludonghua.use.bean.WaterSensor;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

import static org.apache.flink.table.api.Expressions.$;

/**
 * Author Luis
 * DATE 2022-06-19 22:05
 */
public class SQLTest {
    public static void main(String[] args) throws Exception {

        // 1、获取流执行环境
        ParameterTool parameterTool = ExecutionEnvUtil.createParameterTool(args, "/application.properties");
        StreamExecutionEnvironment env = ExecutionEnvUtil.createEnv(parameterTool, false);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // 2、读取端口数据创建流并转换为JavaBean
        SingleOutputStreamOperator<WaterSensor> waterSensorDS = env.socketTextStream("hadoop102", 9999)
                .map(data -> {
                    String[] split = data.split(",");
                    return new WaterSensor(split[0],
                            Long.parseLong(split[1]),
                            Integer.parseInt(split[2]));
                });

        // 3、将流转换为动态表
        Table table = tableEnv.fromDataStream(waterSensorDS);

        // 4、使用SQL查询未注册的表
        Table result = tableEnv.sqlQuery("select id,ts,vc from " + table + " where id='ws_001'");

        // 5、将表对象转换为流进行打印输出
        tableEnv.toAppendStream(result, Row.class).print();

        // 6、执行
        env.execute();

    }
}
