package com.ludonghua.use.sink;

import com.alibaba.fastjson.JSON;
import com.ludonghua.use.bean.WaterSensor;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Properties;

/**
 * Author Luis
 * DATE 2022-06-06 16:55
 */
public class SinkKafka {
    public static void main(String[] args) throws Exception {
        // 1、获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        // 2、读取端口数据并转换为JavaBean
        SingleOutputStreamOperator<WaterSensor> waterSensorDS = env.socketTextStream("hadoop102", 9999)
                .map(new MapFunction<String, WaterSensor>() {
                    @Override
                    public WaterSensor map(String value) throws Exception {
                        String[] split = value.split(",");
                        return new WaterSensor(split[0],
                                Long.parseLong(split[1]),
                                Integer.parseInt(split[2]));
                    }
                });

        // 3、将数据转化为json字符串写入kafka
        SingleOutputStreamOperator<String> result = waterSensorDS.map(new MapFunction<WaterSensor, String>() {
            @Override
            public String map(WaterSensor value) throws Exception {
                return JSON.toJSONString(value);
            }
        });

        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "hadoop102:9092");
        result.addSink(new FlinkKafkaProducer<String>("test", new SimpleStringSchema(), properties));

        // 4、执行
        env.execute();
    }
}
