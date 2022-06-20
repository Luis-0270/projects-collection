package com.ludonghua.use.flinksql.SQL.overwindow;

import com.ludonghua.common.utils.ExecutionEnvUtil;
import com.ludonghua.use.bean.WaterSensor;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import static org.apache.flink.table.api.Expressions.$;

/**
 * Author Luis
 * DATE 2022-06-21 00:07
 */
public class OverWindow {
    public static void main(String[] args) throws Exception {
        // 1、获取流执行环境
        ParameterTool parameterTool = ExecutionEnvUtil.createParameterTool(args, "/application.properties");
        StreamExecutionEnvironment env = ExecutionEnvUtil.createEnv(parameterTool, false);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // 2、读取端口数据创建流并转换为JavaBean
        SingleOutputStreamOperator<WaterSensor> waterSensorDS = env.socketTextStream("hadoop102", 9999)
                .map(data -> {
                    String[] split = data.split(",");
                    return new WaterSensor(split[0],
                            Long.parseLong(split[1]),
                            Integer.parseInt(split[2]));
                });

        // 3、将流转换为表并指定处理时间
        Table table = tableEnv.fromDataStream(waterSensorDS,
                $("id"),
                $("ts"),
                $("vc"),
                $("pt").proctime());

        // 4、SQL API 实现滑动动时间窗口
//        Table result = tableEnv.sqlQuery("select " +
//                "id," +
//                "sum(vc) over(partition by id order by pt) sum_vc, " +
//                "count(id) over(partition by id order by pt) ct " +
//                "from " + table);

        Table result = tableEnv.sqlQuery("select " +
                "id," +
                "sum(vc) over w as sum_vc, " +
                "count(id) over w as ct " +
                "from " + table +
                " window w as (partition by id order by pt rows between 2 preceding and current row)");

        // 5、将结果转换成流输出
//        tableEnv.toAppendStream(result, Row.class).print();
        result.execute().print();

        // 6、执行
        env.execute();
    }
}
