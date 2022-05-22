package com.ludonghua.flume.interceptor;

import com.alibaba.fastjson.JSONObject;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.interceptor.Interceptor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class TimestampInterceptor implements Interceptor {
    @Override
    public void initialize() {

    }

    // 为单个Event添加时间戳
    @Override
    public Event intercept(Event event) {
        // 获取输入的Event的header 和 body
        Map<String, String> headers = event.getHeaders();
        byte[] body = event.getBody();

        // 从Body中读取日志生成的时间，写header
        String line = new String(body, StandardCharsets.UTF_8);
        JSONObject jsonObject = JSONObject.parseObject(line);
        String ts = jsonObject.getString("ts");
        headers.put("timestamp", ts);

//        Long ts = jsonObject.getLong("ts");
//        headers.put("timestamp", String.valueOf(ts*1000));

        return event;

    }

    // 批量为Event添加时间戳
    @Override
    public List<Event> intercept(List<Event> list) {
        for (Event event : list) {
            intercept(event);
        }
        return list;
    }

    @Override
    public void close() {

    }

    public static class Builder implements Interceptor.Builder {

        @Override
        public Interceptor build() {
            return new TimestampInterceptor();
        }

        @Override
        public void configure(Context context) {

        }
    }
}
