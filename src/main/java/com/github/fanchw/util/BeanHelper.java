package com.github.fanchw.util;

import com.github.fanchw.entity.BeanProperty;
import com.github.fanchw.exception.BeanResolveException;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BeanHelper {
    private static final String GET = "get";

    private static final String SET = "set";

    /**
     * 根据类类型获取bean属性
     */
    public static <T> BeanProperty<T> getBeanProperty(Class<T> clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        BeanProperty<T> beanProperty = new BeanProperty<>();
        if (declaredFields.length < 1) {
            return beanProperty;
        }
        List<String> fieldList = new ArrayList<>();
        Map<String, Method> getMethod = new HashMap<>();
        Map<String, Method> setMethod = new HashMap<>();
        try {
            for (Field f : declaredFields) {
                String fieldName = f.getName();
                fieldList.add(fieldName);
                char[] chars = fieldName.toCharArray();
                chars[0] -= 32;
                String upperCamel = new String(chars);
                String get = GET + upperCamel;
                String set = SET + upperCamel;
                getMethod.put(fieldName, clazz.getMethod(get));
                Class<?> type = f.getType();
                setMethod.put(fieldName, clazz.getMethod(set, type));
            }
        } catch (Exception e) {
            log.error("Not a standard bean!");
            throw new BeanResolveException("Not a standard bean!");
        }

        return beanProperty.setFieldName(fieldList).setGetMethod(getMethod).setSetMethod(setMethod);
    }

    /**
     * 根据对象获取bean属性
     */
    @SuppressWarnings("unchecked")
    public static <T> BeanProperty<T> getBeanProperty(T t) {
        return t == null ? new BeanProperty<T>() : (BeanProperty<T>) getBeanProperty(t.getClass());
    }

    /**
     * 根据类类型判断是否为Bean
     */
    public static boolean isBean(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        Field[] declaredFields = clazz.getDeclaredFields();

        for (Field f : declaredFields) {
            String fieldName = f.getName();
            char[] chars = fieldName.toCharArray();
            chars[0] -= 32;
            String upperCamel = new String(chars);
            String get = GET + upperCamel;
            try {
                clazz.getMethod(get);
            } catch (NoSuchMethodException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断对象是否为Bean
     */
    public static <T> boolean isBean(T t) {
        return t != null && isBean(t.getClass());
    }

    /**
     * 生成一个Bean对象，从另一个Bean对象中复制属性名称类型想同的属性
     */
    @SuppressWarnings("all")
    public static <U, V> V beanTrans(U u, Class<V> vClass) {
        V v = null;
        if (u == null || vClass == null) {
            return v;
        }
        Class<?> uClass = u.getClass();
        try {
            BeanProperty<?> uBeanProperty = getBeanProperty(uClass);
            BeanProperty<V> vBeanProperty = getBeanProperty(vClass);
            Map<String, Method> vSetMethod = vBeanProperty.getSetMethod();
            List<String> vFieldName = vBeanProperty.getFieldName();
            Map<String, Method> uGetMethod = uBeanProperty.getGetMethod();
            v = vClass.newInstance();
            for (String vf : vFieldName) {
                Method vSet = vSetMethod.get(vf);
                Method uGet = uGetMethod.get(vf);
                if (vSet == null || uGet == null) {
                    continue;
                }
                vSet.invoke(v, uGet.invoke(u));
            }

        } catch (Exception e) {
            log.error("Bean trans error!");
            throw new BeanResolveException("Please check Bean");
        }
        return v;
    }

    /**
     * 复制一个Bean对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T beanCopy(T t) {
        return t == null ? null : (T) beanTrans(t, t.getClass());
    }

    /**
     * 将map对象转为指定类型Bean对象
     */
    @SuppressWarnings("all")
    public static <T> T mapToBean(Map<String, Object> map, Class<T> tClass) {
        T t = null;
        try {
            if (tClass == null || map == null) {
                return t;
            }
            t = tClass.newInstance();
            BeanProperty<T> beanProperty = getBeanProperty(tClass);
            List<String> fieldName = beanProperty.getFieldName();
            if (fieldName == null || fieldName.size() < 1) {
                return t;
            }
            Map<String, Method> setMethod = beanProperty.getSetMethod();
            for (String filed : fieldName) {
                Method method = setMethod.get(filed);
                Object data = map.get(filed);
                if (method == null || data == null) {
                    continue;
                }
                method.invoke(t, data);
            }
        } catch (Exception e) {
            log.error("MapToBean error!");
            throw new BeanResolveException("Please check Map and Bean!");
        }

        return t;
    }


    /**
     * 将Bean对象转为Map对象
     */

    public static <T> Map<String, Object> mapToBean(T t) {
        Map<String, Object> data = new HashMap<>();
        if (t == null) {
            return null;
        }
        try {
            Class<?> tClass = t.getClass();
            BeanProperty<?> beanProperty = getBeanProperty(tClass);
            List<String> fieldName = beanProperty.getFieldName();
            if (fieldName == null || fieldName.size() < 1) {
                return data;
            }
            Map<String, Method> getMethod = beanProperty.getGetMethod();
            for (String filed : fieldName) {
                Method method = getMethod.get(filed);
                if (method == null) {
                    continue;
                }
                data.put(filed, method.invoke(t));
            }
        } catch (Exception e) {
            log.error("BeanToMap error!");
            throw new BeanResolveException("Please check Bean!");
        }
        return data;
    }

}
