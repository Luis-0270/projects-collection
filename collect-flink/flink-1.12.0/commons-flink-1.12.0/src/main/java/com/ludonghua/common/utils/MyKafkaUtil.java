package com.ludonghua.common.utils;

import com.ludonghua.common.constant.PropertiesConstants;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

import java.util.Properties;

public class MyKafkaUtil {

    public static Properties buildKafkaProps(ParameterTool parameterTool) {
        Properties props = new Properties();
        props.put("bootstrap.servers", parameterTool.get(PropertiesConstants.KAFKA_BROKERS));
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("auto.offset.reset", "latest");
        return props;
    }

    /**
     * 获取生产者对象
     */
    public static FlinkKafkaProducer<String> getFlinkKafkaProducer(String topic, Properties props) {

        return new FlinkKafkaProducer<String>(topic,
                new SimpleStringSchema(),
                props);
    }

    /**
     * 获取消费者对象
     */
    public static FlinkKafkaConsumer<String> getFlinkKafkaConsumer(String topic, String groupId, Properties props) {

        //添加消费组属性
        props.setProperty("group.id", groupId);

        return new FlinkKafkaConsumer<String>(topic,
                new SimpleStringSchema(),
                props);
    }

}
