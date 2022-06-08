package com.ludonghua.use.state;

import com.ludonghua.use.bean.WaterSensor;
import org.apache.commons.compress.utils.Lists;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.*;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.*;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;

/**
 * Author Luis
 * DATE 2022-06-08 15:14
 */
public class StateBroadStateOP {
    public static void main(String[] args) throws Exception {
        // 1、获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(3);

        // 2、读取流中的数据
        DataStreamSource<String> propertiesStream = env.socketTextStream("hadoop102", 8888);
        DataStreamSource<String> dataStream = env.socketTextStream("hadoop102", 9999);

        // 3、定义状态并广播
        MapStateDescriptor<String, String> mapStateDescriptor = new MapStateDescriptor<>("map-state", String.class, String.class);
        BroadcastStream<String> broadcast = propertiesStream.broadcast(mapStateDescriptor);

        // 4、连接数据和广播流
        BroadcastConnectedStream<String, String> connectedStream = dataStream.connect(broadcast);

        // 5、处理连接之后的流
        connectedStream.process(new BroadcastProcessFunction<String, String, String>() {
            @Override
            public void processElement(String value, ReadOnlyContext ctx, Collector<String> out) throws Exception {
                // 获取广播状态
                ReadOnlyBroadcastState<String, String> broadcastState = ctx.getBroadcastState(mapStateDescriptor);
                String aSwitch = broadcastState.get("Switch");

                if("1".equals(aSwitch)) {
                    out.collect("读取了广播流1");
                }else if ("2".equals(aSwitch)) {
                    out.collect("读取了广播流2");
                }else {
                    out.collect("读取了广播流其他");
                }
            }

            @Override
            public void processBroadcastElement(String value, Context ctx, Collector<String> out) throws Exception {
                BroadcastState<String, String> broadcastState = ctx.getBroadcastState(mapStateDescriptor);
                broadcastState.put("Switch", value);
            }
        }).print();

        // 6、执行
        env.execute();
    }
}
