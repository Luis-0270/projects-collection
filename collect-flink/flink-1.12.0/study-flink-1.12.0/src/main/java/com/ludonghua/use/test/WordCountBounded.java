package com.ludonghua.use.test;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

/**
 * Author Luis
 * DATE 2022-06-04 23:01
 */
public class WordCountBounded {
    public static void main(String[] args) throws Exception {
        // 1、获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        // 2、读取文件数据
        DataStreamSource<String> input = env.readTextFile("input/word.txt");

        // 3、压平
        SingleOutputStreamOperator<Tuple2<String, Integer>> wordToOneDS = input.flatMap(new LineToTupleFlatMapFunc());

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
        env.execute("WordCountBounded");
    }

    public static class LineToTupleFlatMapFunc implements FlatMapFunction<String, Tuple2<String, Integer>> {

        @Override
        public void flatMap(String s, Collector<Tuple2<String, Integer>> collector) throws Exception {
            // 按照空格切割
            String[] words = s.split(" ");

            // 遍历words, 写出一个个的单词
            for (String word : words) {
                collector.collect(new Tuple2<>(word, 1));
            }
        }
    }
}
