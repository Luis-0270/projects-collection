package com.ludonghua.use.practice;

import com.ludonghua.use.bean.OrderEvent;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

/**
 * Author Luis
 * DATE 2022-06-11 20:22
 */
public class OrderPay {
    public static void main(String[] args) throws Exception {
        // 1、获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        // 2、读取文本数据创建流并转换为JavaBean对象提取时间戳生产WaterMark
        WatermarkStrategy<OrderEvent> orderEventWatermarkStrategy = WatermarkStrategy.<OrderEvent>forMonotonousTimestamps().withTimestampAssigner(new SerializableTimestampAssigner<OrderEvent>() {
            @Override
            public long extractTimestamp(OrderEvent element, long recordTimestamp) {
                return element.getEventTime() * 1000L;
            }
        });
        SingleOutputStreamOperator<OrderEvent> orderEventDS = env.readTextFile("input/OrderLog.csv")
                .map(data -> {
                    String[] split = data.split(",");
                    return new OrderEvent(Long.parseLong(split[0]),
                            split[1],
                            split[2],
                            Long.parseLong(split[3]));
                }).assignTimestampsAndWatermarks(orderEventWatermarkStrategy);

        // 3、按照OrderID进行分组
        KeyedStream<OrderEvent, Long> keyedStream = orderEventDS.keyBy(OrderEvent::getOrderId);

        // 4、使用状态编程 + 定时器方式实现超时订单的获取
        SingleOutputStreamOperator<String> result = keyedStream.process(new OrderPayProcessFunc(15));

        // 5、打印
        result.print();
        result.getSideOutput(new OutputTag<String>("Pay TimeOut or No Create"){}).print("Pay TimeOut or No Create");
        result.getSideOutput(new OutputTag<String>("No Pay"){}).print("No Pay");

        // 6、执行
        env.execute();
    }

    public static class OrderPayProcessFunc extends KeyedProcessFunction<Long, OrderEvent, String> {

        private Integer interval;

        public OrderPayProcessFunc(Integer interval) {
            this.interval = interval;
        }

        // 声明状态
        private ValueState<OrderEvent> createState;
        private ValueState<Long> timerState;

        @Override
        public void open(Configuration parameters) throws Exception {
            createState = getRuntimeContext().getState(new ValueStateDescriptor<OrderEvent>("create-state", OrderEvent.class));
            timerState = getRuntimeContext().getState(new ValueStateDescriptor<Long>("ts-state", Long.class));
        }

        @Override
        public void processElement(OrderEvent value, Context ctx, Collector<String> out) throws Exception {

            // 判断当前的数据中的类型
            if ("create".equals(value.getEventType())) {

                // 更新状态
                createState.update(value);

                // 注册interval分钟以后的定时器
                long ts = (value.getEventTime() + interval * 60) * 1000L;
                ctx.timerService().registerEventTimeTimer(ts);

                timerState.update(ts);
            } else if ("pay".equals(value.getEventType())) {

                // 取出状态中的数据
                OrderEvent orderEvent = createState.value();

                // 判断创建数据是否为NULL
                if (orderEvent == null) {

                    // 丢失了创建数据, 或者超过了15分钟才支付
                    ctx.output(new OutputTag<String>("Pay TimeOut or No Create"){},
                            value.getOrderId() + "Pay But No Create");
                } else {

                    // 结合写出
                    out.collect(value.getOrderId() + " Create at " + orderEvent.getEventTime() +
                            " Payed at " + value.getEventTime());

                    // 删除定时器
                    ctx.timerService().deleteEventTimeTimer(timerState.value());

                    // 清空状态
                    createState.clear();
                    timerState.clear();
                }
            }
        }

        @Override
        public void onTimer(long timestamp, OnTimerContext ctx, Collector<String> out) throws Exception {

            // 取出状态中的数据
            OrderEvent orderEvent = createState.value();

            ctx.output(new OutputTag<String>("No Pay"){},
                    orderEvent.getOrderId() + " Create But No Pay");

            // 清空状态
            createState.clear();
            timerState.clear();
        }
    }
}
