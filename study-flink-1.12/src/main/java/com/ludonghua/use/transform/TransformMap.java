package com.ludonghua.use.transform;

import com.ludonghua.use.bean.WaterSensor;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * Author Luis
 * DATE 2022-06-06 12:10
 */
public class TransformMap {
    public static void main(String[] args) throws Exception {
        // 1、获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        // 2、从文件中读取数据
        DataStreamSource<String> stringDataStreamSource = env.readTextFile("input/sensor.txt");

        // 3、打印数据
        stringDataStreamSource.map(new MyMapFunc()).print();

        // 4、执行
        env.execute();
    }

    public static class MyMapFunc implements MapFunction<String, WaterSensor> {
        @Override
        public WaterSensor map(String s) throws Exception {
            String[] split = s.split(",");
            return new WaterSensor(split[0], Long.parseLong(split[1]), Integer.parseInt(split[2]));
        }
    }
}
