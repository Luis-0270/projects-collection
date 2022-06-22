package com.ludonghua.use.flinksql.function;

import com.ludonghua.common.utils.ExecutionEnvUtil;
import com.ludonghua.use.bean.SumCount;
import com.ludonghua.use.bean.VcTop2;
import com.ludonghua.use.bean.WaterSensor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.functions.AggregateFunction;
import org.apache.flink.table.functions.TableAggregateFunction;
import org.apache.flink.util.Collector;

import static org.apache.flink.table.api.Expressions.$;
import static org.apache.flink.table.api.Expressions.call;

/**
 * Author Luis
 * DATE 2022-06-22 22:16
 */
public class FunctionUDTAF {
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
        tableEnv.createTemporarySystemFunction("top2", Top2.class);

        // TableAPI
        table.groupBy($("id"))
                .flatAggregate(call("top2", $("vc")).as("top", "rank"))
                .select($("id"), $("top"), $("rank"))
                .execute().print();

        // 执行
        env.execute();

    }

    public static class Top2 extends TableAggregateFunction<Tuple2<Integer, String>, VcTop2> {

        @Override
        public VcTop2 createAccumulator() {
            return new VcTop2(Integer.MIN_VALUE, Integer.MIN_VALUE);
        }

        public void accumulate(VcTop2 acc, Integer value) {

            if (value > acc.getTopOne()) {
                acc.setTopTwo(acc.getTopOne());
                acc.setTopOne(value);
            } else if (value > acc.getTopTwo()) {
                acc.setTopTwo(value);
            }
        }

        public void emitValue(VcTop2 acc, Collector<Tuple2<Integer, String>> out) {
            out.collect(new Tuple2<>(acc.getTopOne(), "Top1"));
            if (acc.getTopTwo() > Integer.MIN_VALUE) {
                out.collect(new Tuple2<>(acc.getTopTwo(), "Top2"));
            }

        }
    }
}
