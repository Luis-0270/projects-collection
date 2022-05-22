package com.ludonghua.flume.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;

/**
 *  判断log是否是json
 */
public class JSONUtil {
    public static boolean isJSONValidate(String log){
        try {
            JSON.parseObject(log);
            return true;
        }catch (JSONException e){
            return false;
        }
    }
}

