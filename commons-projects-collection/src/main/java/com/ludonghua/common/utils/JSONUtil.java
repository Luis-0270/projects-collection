package com.ludonghua.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;

public class JSONUtil {
    public static boolean isJSONValidate(String log){
        try {
            return JSON.parseObject(log) != null;
        }catch (JSONException e){
            return false;
        }
    }
}



