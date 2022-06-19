package com.ludonghua.use.test;

import com.ludonghua.common.utils.ExecutionEnvUtil;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * Author Luis
 * DATE 2022-06-05 14:59
 */
public class WordCountUnbounded {
    public static void main(String[] args) throws Exception {
        // 1、获取执行环境
        ParameterTool parameterTool = ExecutionEnvUtil.createParameterTool(args, "/application.properties");
        StreamExecutionEnvironment env = ExecutionEnvUtil.createEnv(parameterTool, true);
        env.setParallelism(4);
        // 2、读取端口数据创建流
        DataStreamSource<String> socketTextStream = env.socketTextStream("hadoop102", 9999);

        // 3、压平
        SingleOutputStreamOperator<Tuple2<String, Integer>> wordToOneDS = socketTextStream.flatMap(new WordCountBounded.LineToTupleFlatMapFunc());

        // 4、分组
        KeyedStream<Tuple2<String, Integer>, String> keyedStream = wordToOneDS.keyBy(new KeySelector<Tuple2<String, Integer>, String>() {
            @Override
            public String getKey(Tuple2<String, Integer> stringIntegerTuple2) throws Exception {
                return stringIntegerTuple2.f0;
            }
        });

        // 5、按照key做聚合
        SingleOutputStreamOperator<Tuple2<String, Integer>> result = keyedStream.sum(1);

        // 6、打印结果
        result.print();

        // 7、启动任务
        env.execute("WordCountUnbounded");
    }
}
