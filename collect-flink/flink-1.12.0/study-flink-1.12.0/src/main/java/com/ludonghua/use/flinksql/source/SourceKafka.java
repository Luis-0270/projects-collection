package com.ludonghua.use.flinksql.source;

import com.ludonghua.common.utils.ExecutionEnvUtil;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.descriptors.Csv;
import org.apache.flink.table.descriptors.Kafka;
import org.apache.flink.table.descriptors.Schema;
import org.apache.flink.types.Row;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import static org.apache.flink.table.api.Expressions.$;

/**
 * Author Luis
 * DATE 2022-06-18 21:33
 */
public class SourceKafka {
    public static void main(String[] args) throws Exception {

        // 1、获取执行环境
        ParameterTool parameterTool = ExecutionEnvUtil.createParameterTool(args, "/application.properties");
        StreamExecutionEnvironment env = ExecutionEnvUtil.createEnv(parameterTool, true);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // 2、使用连接器的方式读取Kafka的数据
        tableEnv.connect(new Kafka()
                .version("universal")
                .topic("test")
                .startFromLatest()
                .property(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "hadoop102:9092")
                .property(ConsumerConfig.GROUP_ID_CONFIG, "test"))
                .withSchema(new Schema()
                    .field("id", DataTypes.STRING())
                    .field("ts", DataTypes.BIGINT())
                    .field("vc", DataTypes.INT()))
                .withFormat(new Csv())
                .createTemporaryTable("sensor");

        // 3、使用连接器创建表
        Table sensor = tableEnv.from("sensor");

        // 4、查询数据
        Table resultTable = sensor.groupBy($("id"))
                .select($("id"), $("id").count());

        // 5、将表转换为流进行输出
        tableEnv.toRetractStream(resultTable, Row.class).print();

        // 6、执行
        env.execute();

    }
}
