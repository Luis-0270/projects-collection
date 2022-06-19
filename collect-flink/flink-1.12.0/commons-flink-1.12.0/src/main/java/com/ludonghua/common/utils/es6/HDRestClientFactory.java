package com.ludonghua.common.utils.es6;

import org.apache.flink.streaming.connectors.elasticsearch6.RestClientFactory;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClientBuilder;

public class HDRestClientFactory implements RestClientFactory {

    private String userName;
    private String password;
    transient CredentialsProvider credentialsProvider;

    public HDRestClientFactory(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    @Override
    public void configureRestClientBuilder(RestClientBuilder restClientBuilder) {
        if (credentialsProvider == null) {
            credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password));
        }
        restClientBuilder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpAsyncClientBuilder) {
                return httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
        }).setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
            @Override
            public RequestConfig.Builder customizeRequestConfig(RequestConfig.Builder builder) {
                builder.setConnectTimeout(5000);
                builder.setSocketTimeout(60000);
                builder.setConnectionRequestTimeout(2000);
                return builder;
            }
        });
    }
}
