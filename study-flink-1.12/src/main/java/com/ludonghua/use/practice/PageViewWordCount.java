package com.ludonghua.use.practice;

import com.ludonghua.use.bean.UserBehavior;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

/**
 * Author Luis
 * DATE 2022-06-06 22:54
 */
public class PageViewWordCount {
    public static void main(String[] args) throws Exception {
        // 1、获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 2、读取文本数据
        DataStreamSource<String> readTextFile = env.readTextFile("input/UserBehavior.csv");

        // 3、转换为JavaBean并过滤出PV的数据
        SingleOutputStreamOperator<Tuple2<String, Integer>> pv = readTextFile.flatMap(new FlatMapFunction<String, Tuple2<String, Integer>>() {
            @Override
            public void flatMap(String value, Collector<Tuple2<String, Integer>> out) throws Exception {
                // 按照","分割
                String[] split = value.split(",");
                // 封装JavaBean对象
                UserBehavior userBehavior = new UserBehavior(Long.parseLong(split[0]),
                        Long.parseLong(split[1]),
                        Integer.parseInt(split[2]),
                        split[3],
                        Long.parseLong(split[4]));
                // 选择需要输出的数据
                if ("pv".equals(userBehavior.getBehavior())) {
                    out.collect(new Tuple2<>("pv", 1));
                }
            }
        });

        // 4、指定key分组
        KeyedStream<Tuple2<String, Integer>, String> keyedStream = pv.keyBy(data -> data.f0);

        // 5、计算总和
        SingleOutputStreamOperator<Tuple2<String, Integer>> result = keyedStream.sum(1);

        // 6、打印
        result.print();

        // 7、执行
        env.execute();
    }
}
