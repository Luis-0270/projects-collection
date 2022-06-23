package java.com.ludonghua.flume.interceptor;

import com.ludonghua.common.utils.JSONUtil;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.interceptor.Interceptor;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

public class ParseInterceptor implements Interceptor {
    @Override
    public void initialize() {

    }

    // 处理单个event, 不合法json都返回null
    @Override
    public Event intercept(Event event) {

        byte[] body = event.getBody();
        String log = new String(body, StandardCharsets.UTF_8);

        if (JSONUtil.isJSONValidate(log)) {
            return event;
        } else {
            return null;
        }
    }

    // 批次处理数据，不合法的从list中去除
    @Override
    public List<Event> intercept(List<Event> list) {

        Iterator<Event> iterator = list.iterator();
        while (iterator.hasNext()){
            Event event = iterator.next();
            // 将不合法的json从list中移除
            if(intercept(event) == null){
                iterator.remove();
            }
        }

        return list;
    }

    @Override
    public void close() {

    }

    public static class Builder implements Interceptor.Builder {
        @Override
        public Interceptor build() {
            return new ParseInterceptor();
        }
        @Override
        public void configure(Context context) {

        }
    }
}
