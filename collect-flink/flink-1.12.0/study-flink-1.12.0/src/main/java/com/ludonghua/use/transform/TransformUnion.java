package com.ludonghua.use.transform;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * Author Luis
 * DATE 2022-06-06 14:57
 */
public class TransformUnion {
    public static void main(String[] args) throws Exception {
        // 1、获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        // 2、读取端口数据创建流
        DataStreamSource<String> socketTextStream1 = env.socketTextStream("hadoop102", 8888);
        DataStreamSource<String> socketTextStream2 = env.socketTextStream("hadoop102", 9999);

        // 3、连接两条流
        DataStream<String> union = socketTextStream1.union(socketTextStream2);

        // 4、打印
        union.print();

        // 5、执行
        env.execute();

    }
}
