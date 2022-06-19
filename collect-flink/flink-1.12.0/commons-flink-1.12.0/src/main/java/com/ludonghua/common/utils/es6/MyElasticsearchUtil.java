package com.ludonghua.common.utils.es6;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch6.ElasticsearchSink;
import org.apache.http.HttpHost;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MyElasticsearchUtil {

    public static <T> void addSink(List<HttpHost> esAddresses, String userName, String passwd, int bulkFlushMaxActions,
                                   int bulkFlushMaxSizeMb, long bulkFlushInterval, int parallelism,
                                   SingleOutputStreamOperator<T> data, ElasticsearchSinkFunction<T> func) {

        ElasticsearchSink.Builder<T> esSinkBuilder = new ElasticsearchSink.Builder<>(esAddresses, func);

        // 鉴权，正对写 es 需要密码的场景
        if(StringUtils.isNotEmpty(userName) && StringUtils.isNotEmpty(passwd)){
            esSinkBuilder.setRestClientFactory(new HDRestClientFactory(userName, passwd));
        }

        //bulk
        esSinkBuilder.setBulkFlushMaxActions(bulkFlushMaxActions);
        esSinkBuilder.setBulkFlushMaxSizeMb(bulkFlushMaxSizeMb);
        esSinkBuilder.setBulkFlushInterval(bulkFlushInterval);

        data.addSink(esSinkBuilder.build()).setParallelism(parallelism);
    }

    public static List<HttpHost> getEsAddresses(String hosts) throws MalformedURLException {
        String[] hostList = hosts.split(",");
        List<HttpHost> addresses = new ArrayList<>();
        for (String host : hostList) {
            if (host.startsWith("http")) {
                URL url = new URL(host);
                addresses.add(new HttpHost(url.getHost(), url.getPort()));
            } else {
                String[] parts = host.split(":", 2);
                if (parts.length > 1) {
                    addresses.add(new HttpHost(parts[0], Integer.parseInt(parts[1])));
                } else {
                    throw new MalformedURLException("invalid elasticsearch hosts format");
                }
            }
        }
        return addresses;
    }

}
