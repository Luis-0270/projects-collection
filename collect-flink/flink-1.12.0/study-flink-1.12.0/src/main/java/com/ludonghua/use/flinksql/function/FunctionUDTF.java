package com.ludonghua.use.flinksql.function;

import com.ludonghua.common.utils.ExecutionEnvUtil;
import com.ludonghua.use.bean.WaterSensor;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.annotation.DataTypeHint;
import org.apache.flink.table.annotation.FunctionHint;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.functions.ScalarFunction;
import org.apache.flink.table.functions.TableFunction;
import org.apache.flink.types.Row;

import javax.swing.*;

import static org.apache.flink.table.api.Expressions.$;
import static org.apache.flink.table.api.Expressions.call;

/**
 * Author Luis
 * DATE 2022-06-22 22:16
 */
public class FunctionUDTF {
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


        // 4、注册自定义函数再使用
        tableEnv.createTemporarySystemFunction("split", Split.class);

        // TableAPI
//        table.joinLateral(call("split", $("id")))
//                .select($("id"), $("word")).execute().print();

        // SQL
        tableEnv.sqlQuery("select id, word from " + table + ", lateral table(split(id))").execute().print();;

        // 执行
        env.execute();

    }

    @FunctionHint(output = @DataTypeHint("ROW<word String>"))
    public static class Split extends TableFunction<Row> {
        public void eval(String value) {
            String[] split = value.split("_");
            for (String s : split) {
                collect(Row.of(s));
            }
        }
    }
}
