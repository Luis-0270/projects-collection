package com.ludonghua.use.flinksql.join;

import com.ludonghua.common.utils.ExecutionEnvUtil;
import com.ludonghua.use.bean.TableA;
import com.ludonghua.use.bean.TableB;
import com.ludonghua.use.bean.WaterSensor;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.time.Duration;

/**
 * Author Luis
 * DATE 2022-06-22 23:21
 */
public class SQLJoin {
    public static void main(String[] args) throws Exception {
        // 1、获取流执行环境
        ParameterTool parameterTool = ExecutionEnvUtil.createParameterTool(args, "/application.properties");
        StreamExecutionEnvironment env = ExecutionEnvUtil.createEnv(parameterTool, false);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // 默认值为0 FlinkSQL中的状态永久保存
//        tableEnv.getConfig().getIdleStateRetention();

        // 执行FlinkSQL状态保留10秒
        tableEnv.getConfig().setIdleStateRetention(Duration.ofSeconds(10));

        // 2、读取端口数据并转换为JavaBean
        SingleOutputStreamOperator<TableA> aDS = env.socketTextStream("hadoop102", 8888)
                .map(line -> {
                    String[] split = line.split(",");
                    return new TableA(split[0], split[1]);
                });

        SingleOutputStreamOperator<TableB> bDS = env.socketTextStream("hadoop102", 9999)
                .map(line -> {
                    String[] split = line.split(",");
                    return new TableB(split[0], Integer.parseInt(split[1]));
                });

        // 3、将流转换为动态表
        tableEnv.createTemporaryView("tableA", aDS);
        tableEnv.createTemporaryView("tableB", bDS);

        // 4、双流JOIN
        tableEnv.sqlQuery("select * from tableA a left join tableB b on a.id = b.id")
                .execute().print();

        // 5、执行
        env.execute();

    }
}
