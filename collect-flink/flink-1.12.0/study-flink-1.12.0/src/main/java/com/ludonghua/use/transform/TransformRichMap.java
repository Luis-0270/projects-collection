package com.ludonghua.use.transform;

import com.ludonghua.use.bean.WaterSensor;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * Author Luis
 * DATE 2022-06-06 12:15
 */
public class TransformRichMap {
    public static void main(String[] args) throws Exception {
        // 1、获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);

        // 2、从文件中读取数据
        DataStreamSource<String> stringDataStreamSource = env.readTextFile("input/sensor.txt");

        // 3、打印数据
        stringDataStreamSource.map(new MyRichMapFunc())
                .print();

        // 4、执行
        env.execute();
    }

    public static class MyRichMapFunc extends RichMapFunction<String, WaterSensor> {

        @Override
        public void open(Configuration parameters) throws Exception {
            System.out.println("open...");
        }

        @Override
        public WaterSensor map(String value) throws Exception {
            String[] split = value.split(",");
            return new WaterSensor(split[0], Long.parseLong(split[1]), Integer.parseInt(split[2]));
        }

        @Override
        public void close() throws Exception {
            System.out.println("close...");
        }
    }
}
