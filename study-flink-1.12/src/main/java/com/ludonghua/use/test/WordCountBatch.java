package com.ludonghua.use.test;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.*;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Collector;

/**
 * Author Luis
 * DATE 2022-06-04 22:26
 */
public class WordCountBatch {
    public static void main(String[] args) throws Exception {
        // 1、获取执行环境
        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        // 2、读取文件数据
        DataSource<String> input = env.readTextFile("input/sensor.txt");

        // 3、压平
        FlatMapOperator<String, String> wordDS = input.flatMap(new MyFlatMapFunc());

        // 4、将单词装换成元组
        MapOperator<String, Tuple2<String, Integer>> wordToOneDS = wordDS.map(new MapFunction<String, Tuple2<String, Integer>>() {

            @Override
            public Tuple2<String, Integer> map(String s) throws Exception {
//                return new Tuple2<>(s, 1);
                return Tuple2.of(s, 1);
            }
        });

        // 5、分组
        UnsortedGrouping<Tuple2<String, Integer>> groupBy = wordToOneDS.groupBy(0);

        // 6、聚合
        AggregateOperator<Tuple2<String, Integer>> result = groupBy.sum(1);

        // 7、打印结果
        result.print();

    }

    // 自定义实现压平操作的类
    public static class MyFlatMapFunc implements FlatMapFunction<String, String> {

        @Override
        public void flatMap(String s, Collector<String> collector) throws Exception {
            // 按照空格切割
            String[] words = s.split(" ");
            // 遍历words, 写出一个个的单词
            for (String word : words) {
                collector.collect(word);
            }
        }
    }
}
