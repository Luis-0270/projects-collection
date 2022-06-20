package com.ludonghua.use.practice;

import com.google.common.collect.Lists;
import com.ludonghua.common.utils.ExecutionEnvUtil;
import com.ludonghua.use.bean.ItemCount;
import com.ludonghua.use.bean.UserBehavior;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.util.Collector;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;

import static org.apache.flink.table.api.Expressions.$;

/**
 * Author Luis
 * DATE 2022-06-08 22:43
 */
public class ItemCountTopNWithSQL {
    public static void main(String[] args) throws Exception {
        // 1、获取执行环境
        ParameterTool parameterTool = ExecutionEnvUtil.createParameterTool(args, "/application.properties");
        StreamExecutionEnvironment env = ExecutionEnvUtil.createEnv(parameterTool, false);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // 2、读取文本数据
        DataStreamSource<String> readTextFile = env.readTextFile("input-1.12.0/UserBehavior.csv");

        // 3、转换为JavaBean, 根据行为过滤数据, 并提取时间戳生成Watermark
        WatermarkStrategy<UserBehavior> userBehaviorWatermarkStrategy = WatermarkStrategy.<UserBehavior>forMonotonousTimestamps().withTimestampAssigner(new SerializableTimestampAssigner<UserBehavior>() {
            @Override
            public long extractTimestamp(UserBehavior element, long recordTimestamp) {
                return element.getTimestamp() * 1000L;
            }
        });
        SingleOutputStreamOperator<UserBehavior> userBehaviorDS = readTextFile.map(data -> {
            String[] split = data.split(",");
            return new UserBehavior(Long.parseLong(split[0]),
                    Long.parseLong(split[1]),
                    Integer.parseInt(split[2]),
                    split[3],
                    Long.parseLong(split[4]));
        }).filter(data -> "pv".equals(data.getBehavior()))
                .assignTimestampsAndWatermarks(userBehaviorWatermarkStrategy);

        // 4、将userBehaviorDS流转换成动态表并指定事件事件字段
        Table table = tableEnv.fromDataStream(userBehaviorDS,
                $("userId"),
                $("itemId"),
                $("categoryId"),
                $("behavior"),
                $("timestamp"),
                $("rt").rowtime());

        // 5、使用Flink SQL 实现滑动窗口计算每个商品被点击的总数
        Table windowItemCountTalbe = tableEnv.sqlQuery("select " +
                " itemId," +
                " count(itemId) as ct," +
                " hop_end(rt, INTERVAL '5' minute, INTERVAL '1' hour) as windowEnd " +
                " from " + table +
                " group by itemId,hop(rt, INTERVAL '5' minute, INTERVAL '1' hour)");

        // 6、按照窗口关闭时间分组, 排序
        Table rankTable = tableEnv.sqlQuery("select " +
                " itemId," +
                "ct," +
                " windowEnd," +
                " row_number() over(partition by windowEnd order by ct desc) as rk" +
                " from " + windowItemCountTalbe);

        // 7、取TopN
        Table result = tableEnv.sqlQuery("select * from " + rankTable + " where rk<=5");

        // 8、打印结果
        result.execute().print();

        // 9、执行
        env.execute();
    }
}
