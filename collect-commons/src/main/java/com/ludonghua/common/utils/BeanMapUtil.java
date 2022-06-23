package com.ludonghua.common.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class BeanMapUtil {

    public static Map<String, Object> beanToMapVNotNull(Object object) throws IllegalAccessException {

        Map<String, Object> map = new HashMap<>();
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            // 这里为了不写入es的数据值为null
            if (field.get(object) != null) {
                map.put(field.getName(), field.get(object));
            }
        }
        return map;
    }

    public static <T> T mapToBean(Map<String, Object> map, Class<T> beanClass) throws Exception {
        T object = beanClass.newInstance();
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            int mod = field.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isFinal(mod)) {
                continue;
            }

            field.setAccessible(true);
            if (map.containsKey(field.getName())) {
                field.set(object, map.get(field.getName()));
            }
        }
        return object;
    }
}