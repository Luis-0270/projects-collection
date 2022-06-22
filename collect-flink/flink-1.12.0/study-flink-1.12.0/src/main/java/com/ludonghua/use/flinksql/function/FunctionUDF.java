package com.ludonghua.use.flinksql.function;

import com.ludonghua.common.utils.ExecutionEnvUtil;
import com.ludonghua.use.bean.WaterSensor;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.functions.ScalarFunction;

import static org.apache.flink.table.api.Expressions.$;
import static org.apache.flink.table.api.Expressions.call;

/**
 * Author Luis
 * DATE 2022-06-22 22:16
 */
public class FunctionUDF {
    public static void main(String[] args) throws Exception {
        // 1、获取流执行环境
        ParameterTool parameterTool = ExecutionEnvUtil.createParameterTool(args, "/application.properties");
        StreamExecutionEnvironment env = ExecutionEnvUtil.createEnv(parameterTool, false);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // 2、读取端口数据并转换为JavaBean
        SingleOutputStreamOperator<WaterSensor> waterSensorDS = env.socketTextStream("hadoop102", 9999)
                .map(data -> {
                    String[] split = data.split(",");
                    return new WaterSensor(split[0],
                            Long.parseLong(split[1]),
                            Integer.parseInt(split[2]));
                });

        // 3、将流转换为动态表
        Table table = tableEnv.fromDataStream(waterSensorDS);

        // 4、不注册函数直接使用
//        table.select(call(MyLength.class, $("id"))).execute().print();

        // 5、注册自定义函数再使用
        tableEnv.createTemporarySystemFunction("mylen", MyLength.class);

        // TableAPI
//        table.select($("id"), call("mylen", $("id"))).execute().print();

        // SQL
        tableEnv.sqlQuery("select id, mylen(id) from " + table).execute().print();;

        // 执行
        env.execute();

    }

    public static class MyLength extends ScalarFunction {
        public int eval(String value) {
            return value.length();
        }
    }
}
