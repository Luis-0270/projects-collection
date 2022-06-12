package com.ludonghua.use.practice;

import com.google.common.collect.Lists;
import com.ludonghua.use.bean.LoginEvent;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Author Luis
 * DATE 2022-06-11 17:56
 */
public class LoginApp2 {
    public static void main(String[] args) throws Exception {
        // 1、获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        // 2、读取文本数据创建流并转换为JavaBean对象提取时间戳生产WaterMark
        WatermarkStrategy<LoginEvent> loginEventWatermarkStrategy = WatermarkStrategy.<LoginEvent>forBoundedOutOfOrderness(Duration.ofSeconds(10)).withTimestampAssigner(new SerializableTimestampAssigner<LoginEvent>() {
            @Override
            public long extractTimestamp(LoginEvent element, long recordTimestamp) {
                return element.getEventTime() * 1000L;
            }
        });
        SingleOutputStreamOperator<LoginEvent> loginEventDS = env.readTextFile("input/LoginLog.csv")
                .map(data -> {
                    String[] split = data.split(",");
                    return new LoginEvent(Long.parseLong(split[0]),
                            split[1],
                            split[2],
                            Long.parseLong(split[3]));
                }).assignTimestampsAndWatermarks(loginEventWatermarkStrategy);

        // 3、按照用户ID分组
        KeyedStream<LoginEvent, Long> keyedStream = loginEventDS.keyBy(LoginEvent::getUserId);

        // 4、使用processAPI, 状态, 定时器
        SingleOutputStreamOperator<String> result = keyedStream.process(new LoginKeyedProcessFunc(2));

        // 5、打印
        result.print();

        // 6、执行
        env.execute();
    }

    public static class LoginKeyedProcessFunc extends KeyedProcessFunction<Long, LoginEvent, String> {

        // 定义属性
        private  Integer ts;

        public LoginKeyedProcessFunc(Integer ts) {
            this.ts = ts;
        }

        // 声明状态
        private ValueState<LoginEvent> failEventState;

        @Override
        public void open(Configuration parameters) throws Exception {
            failEventState = getRuntimeContext().getState(new ValueStateDescriptor<LoginEvent>("ts-state", LoginEvent.class));
        }

        @Override
        public void processElement(LoginEvent value, Context ctx, Collector<String> out) throws Exception {

            // 取出状态中的数据
            if ("fail".equals(value.getEventType())) {

                // 取出状态中的数据
                LoginEvent loginEvent = failEventState.value();

                // 更新状态
                failEventState.update(value);

                // 如果非第一条失败数据并且时间间隔小于等于ts值, 则输出报警信息
                if (loginEvent != null && Math.abs(value.getEventTime() - loginEvent.getEventTime()) <= ts) {

                    // 输出报警信息
                    out.collect(value.getUserId() + "连续登陆失败2次");
                }

            } else {
                // 成功数据, 清空状态
                failEventState.clear();
            }

        }

    }
}
