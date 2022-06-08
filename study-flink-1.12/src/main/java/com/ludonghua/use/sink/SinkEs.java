package com.ludonghua.use.sink;

import com.alibaba.fastjson.JSON;
import com.ludonghua.use.bean.WaterSensor;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkBase;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.streaming.connectors.elasticsearch6.ElasticsearchSink;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.http.HttpHost;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 * Author Luis
 * DATE 2022-06-06 20:02
 */
public class SinkEs {
    public static void main(String[] args) throws Exception {
        // 1、获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        // 2、读取端口数据并转换为JavaBean
        SingleOutputStreamOperator<WaterSensor> waterSensorDS = env.socketTextStream("hadoop102", 9999)
//        SingleOutputStreamOperator<WaterSensor> waterSensorDS = env.readTextFile("input/sensor.txt")
                .map(new MapFunction<String, WaterSensor>() {
                    @Override
                    public WaterSensor map(String value) throws Exception {
                        String[] split = value.split(",");
                        return new WaterSensor(split[0],
                                Long.parseLong(split[1]),
                                Integer.parseInt(split[2]));
                    }
                });

        // 3、将数据串写入es
        ArrayList<HttpHost> httpHosts = new ArrayList<>();
        httpHosts.add(new HttpHost("hadoop102", 9200));
        ElasticsearchSink.Builder<WaterSensor> waterSensorBuilder = new ElasticsearchSink.Builder<WaterSensor>(httpHosts, new MyEsSinkFunc());
        // 每条刷写一次
        waterSensorBuilder.setBulkFlushMaxActions(1);
        // 每10秒刷写一次
        waterSensorBuilder.setBulkFlushInterval(10000);
        ElasticsearchSink<WaterSensor> elasticsearchSink = waterSensorBuilder.build();
        waterSensorDS.addSink(elasticsearchSink);

        // 4、执行
        env.execute();
    }

    public static class MyEsSinkFunc implements ElasticsearchSinkFunction<WaterSensor> {

        @Override
        public void process(WaterSensor element, RuntimeContext ctx, RequestIndexer indexer) {

            HashMap<String, String> source = new HashMap<>();
            source.put("ts", element.getTs().toString());
            source.put("vc", element.getVc().toString());

            // 创建Index请求
            IndexRequest indexRequest = Requests.indexRequest()
                    .index("sensor")
                    .type("_doc")
                    .id(element.getId())
                    .source(source);

            // 写入ES
            indexer.add(indexRequest);
        }
    }
}
