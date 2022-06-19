package com.ludonghua.use.practice;

import com.ludonghua.use.bean.UserBehavior;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.HashSet;

/**
 * Author Luis
 * DATE 2022-06-06 23:31
 */
public class UserVisitor {
    public static void main(String[] args) throws Exception {
        // 1、获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 2、读取文本数据
        DataStreamSource<String> readTextFile = env.readTextFile("input/UserBehavior.csv");

        // 3、转换为JavaBean并过滤出PV的数据
        SingleOutputStreamOperator<UserBehavior> userBehaviorDS = readTextFile.flatMap(new FlatMapFunction<String, UserBehavior>() {
            @Override
            public void flatMap(String value, Collector<UserBehavior> out) throws Exception {
                // 按照","分割
                String[] split = value.split(",");
                // 封装JavaBean对象
                UserBehavior userBehavior = new UserBehavior(Long.parseLong(split[0]),
                        Long.parseLong(split[1]),
                        Integer.parseInt(split[2]),
                        split[3],
                        Long.parseLong(split[4]));
                // 选择需要输出的数据
                if ("pv".equals(userBehavior.getBehavior())) {
                    out.collect(userBehavior);
                }
            }
        });

        // 4、指定key分组
        KeyedStream<UserBehavior, String> keyedStream = userBehaviorDS.keyBy(data -> "UV");

        // 5、使用Process方式计算总和(userId去重)
        SingleOutputStreamOperator<Integer> result = keyedStream.process(new KeyedProcessFunction<String, UserBehavior, Integer>() {

            private HashSet<Long> uids = new HashSet<>();
            private Integer count = 0;

            @Override
            public void processElement(UserBehavior value, Context ctx, Collector<Integer> out) throws Exception {
                if (!uids.contains(value.getUserId())) {
                    uids.add(value.getUserId());
                    count++;
                    out.collect(count);
                }
            }
        });

        // 6、打印结果
        result.print();

        // 7、执行
        env.execute();

    }
}
