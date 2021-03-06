package com.ludonghua.use.transform;

import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

/**
 * Author Luis
 * DATE 2022-06-06 14:09
 */
public class TransformRichFlatMap {
    public static void main(String[] args) throws Exception {
        // 1、获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);

        // 2、从文件中读取数据
        DataStreamSource<String> stringDataStreamSource = env.readTextFile("input/sensor.txt");

        // 3、压平数据
        SingleOutputStreamOperator<String> result = stringDataStreamSource.flatMap(new MyRichFlatMapFunc());

        // 4、打印
        result.print();

        // 5、执行任务
        env.execute();

    }

    public static class MyRichFlatMapFunc extends RichFlatMapFunction<String, String> {

        @Override
        public void open(Configuration parameters) throws Exception {
            System.out.println("open...");
        }

        @Override
        public void flatMap(String value, Collector<String> out) throws Exception {
            String[] split = value.split(",");

            for (String s : split) {
                out.collect(s);
            }
        }
    }
}
