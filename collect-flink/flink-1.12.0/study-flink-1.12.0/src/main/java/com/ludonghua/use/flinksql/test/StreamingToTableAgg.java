package com.ludonghua.use.flinksql.test;

import com.ludonghua.common.utils.ExecutionEnvUtil;
import com.ludonghua.use.bean.WaterSensor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
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
public class StreamingToTableAgg {
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

        // 5、使用TableAPI实现 select id,sum(vc) from sensor where vc >=20 group by id;
        Table selectTable = SensorTable
                .where($("vc").isGreater(20))
                .groupBy($("id"))
                .aggregate($("vc").sum().as("sum_vc"))
                .select($("id"), $("sum_vc"));

        // 6、将selectTable转换为流输出
        DataStream<Tuple2<Boolean, Row>> rowDataStream = tableEnv.toRetractStream(selectTable, Row.class);
        rowDataStream.print();

        // 7、执行
        env.execute();

    }
}
