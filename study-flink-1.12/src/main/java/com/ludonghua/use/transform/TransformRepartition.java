package com.ludonghua.use.transform;

import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * Author Luis
 * DATE 2022-06-06 16:07
 */
public class TransformRepartition {
    public static void main(String[] args) throws Exception {
        // 1、获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 2、读取端口数据
        DataStreamSource<String> socketTextStream = env.socketTextStream("hadoop102", 9999);

        // 3、使用不同的重分区策略分区后打印
        socketTextStream.keyBy(data -> data).print("keyBy");
        socketTextStream.shuffle().print("shuffle");
        socketTextStream.rebalance().print("rebalance");
        socketTextStream.rescale().print("rescale");
        socketTextStream.global().print("global");
//        socketTextStream.broadcast().print("broadcast");

        // 4、执行
        env.execute();
    }
}
