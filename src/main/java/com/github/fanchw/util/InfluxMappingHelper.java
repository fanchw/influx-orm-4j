package com.github.fanchw.util;

import com.github.fanchw.annotation.InfluxFiled;
import com.github.fanchw.annotation.InfluxMeasurement;
import com.github.fanchw.annotation.InfluxTag;
import com.github.fanchw.annotation.InfluxTime;
import com.github.fanchw.entity.BeanProperty;
import com.github.fanchw.entity.InsertMappingEntity;
import com.github.fanchw.entity.MappingMessage;
import com.github.fanchw.entity.MappingMessage.TagAndFieldMapping;
import com.github.fanchw.exception.UnsupportedTimeException;
import org.influxdb.dto.QueryResult;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class InfluxMappingHelper {
    public static final String DEFAULT_MAPPING = "";
    public static final String TIME_COLUMN = "time";

    /**
     * 根据类类型获取表名
     */
    public static String getMeasurement(Class<?> clazz) {
        InfluxMeasurement influxMeasurement = clazz.getAnnotation(InfluxMeasurement.class);
        String measurement;
        if (influxMeasurement == null || DEFAULT_MAPPING.equals(influxMeasurement.value())) {
            measurement = clazz.getSimpleName();
        } else {
            measurement = influxMeasurement.value();
        }
        return measurement;
    }

    /**
     * 映射Bean与Influx表之间的关系
     */
    public static <T> MappingMessage<T> getMappingMessage(Class<T> tClass) {
        Field[] declaredFields = tClass.getDeclaredFields();
        MappingMessage<T> mappingMessage = new MappingMessage<>();
        InfluxMeasurement influxMeasurement = tClass.getAnnotation(InfluxMeasurement.class);
        if (influxMeasurement == null || DEFAULT_MAPPING.equals(influxMeasurement.value())) {
            mappingMessage.setMeasurement(tClass.getSimpleName());
        } else {
            mappingMessage.setMeasurement(influxMeasurement.value());
        }
        Map<String, TagAndFieldMapping> tagAndFieldMappingMap = new HashMap<>();
        for (Field f : declaredFields) {
            TagAndFieldMapping tagAndFieldMapping = new TagAndFieldMapping();
            tagAndFieldMapping.setBeanFieldName(f.getName());
            InfluxTag tag = f.getAnnotation(InfluxTag.class);
            if (tag != null) {
                if (DEFAULT_MAPPING.equals(tag.value())) {
                    tagAndFieldMapping.setColumn(f.getName());
                } else {
                    tagAndFieldMapping.setColumn(tag.value());
                }
                tagAndFieldMappingMap.put(f.getName(), tagAndFieldMapping.setBeanFieldType(f.getType()).setTagFlag(true));
                continue;
            }

            InfluxFiled influxFiled = f.getAnnotation(InfluxFiled.class);
            if (influxFiled != null) {
                if (DEFAULT_MAPPING.equals(influxFiled.value())) {
                    tagAndFieldMapping.setColumn(f.getName());
                } else {
                    tagAndFieldMapping.setColumn(influxFiled.value());
                }
                tagAndFieldMappingMap.put(f.getName(), tagAndFieldMapping.setBeanFieldType(f.getType()).setTagFlag(false));
                continue;
            }
            InfluxTime influxTime = f.getAnnotation(InfluxTime.class);
            if (influxTime != null) {
                mappingMessage.setTimeType(f.getType()).setTimeFieldName(f.getName());
            }
        }
        return mappingMessage.setTagAndFieldMappings(tagAndFieldMappingMap);
    }

    @SuppressWarnings("unchecked")
    public static <T> MappingMessage<T> getMappingMessage(T t) {
        return (MappingMessage<T>) getMappingMessage(t.getClass());
    }

    /**
     * 将influx返回的单个结果映射为Bean对象
     */
    public static <T> T beanMapping(List<String> columns, List<Object> values, Class<T> tClass,
                                    MappingMessage<T> mappingMessage, BeanProperty<T> beanProperty)
            throws Exception {
        T t = tClass.newInstance();
        Map<String, TagAndFieldMapping> tagAndFieldMappings = mappingMessage.getTagAndFieldMappings();
        Map<String, Method> setMethodMap = beanProperty.getSetMethod();
        for (int i = 0; i < columns.size(); i++) {
            String columnName = columns.get(i);
            Object value = values.get(i);
            if (TIME_COLUMN.equals(columnName)) {
                Date javaDate = TimeHelper.influxDateToJavaDate(value.toString());
                Class<?> timeType = mappingMessage.getTimeType();
                String timeFieldName = mappingMessage.getTimeFieldName();
                Method method = setMethodMap.get(timeFieldName);
                if (Long.class.equals(timeType) || long.class.equals(timeType)) {
                    method.invoke(t, javaDate.getTime());
                    continue;
                }
                if (Date.class.equals(timeType)) {
                    method.invoke(t, javaDate);
                    continue;
                }
                if (LocalDateTime.class.equals(timeType)) {
                    method.invoke(t, TimeHelper.millsToLocal(javaDate.getTime()));
                    continue;
                }
                throw new UnsupportedTimeException();
            }
            setMethodMap.get(tagAndFieldMappings.get(columnName).getBeanFieldName()).invoke(t, value);
        }

        return t;
    }

    /**
     * 将influx返回的单个结果映射为Bean对象
     */
    public static <T> T beanMapping(List<String> columns, List<Object> values, Class<T> tClass) throws Exception {
        return beanMapping(columns, values, tClass, getMappingMessage(tClass), BeanHelper.getBeanProperty(tClass));
    }

    /**
     * 将单条语句的查询结果解析为Bean集合
     */
    public static <T> List<T> beanResult(QueryResult queryResult, Class<T> tClass) throws Exception {
        List<QueryResult.Result> results = queryResult.getResults();
        QueryResult.Result result = results.get(0);
        List<T> resultBeanList = new ArrayList<>();
        if (result.getSeries() == null) {
            return resultBeanList;
        }
        List<List<Object>> valueList = result.getSeries().stream().map(QueryResult.Series::getValues).collect(Collectors.toList()).get(0);
        List<String> columns = result.getSeries().stream().map(QueryResult.Series::getColumns).collect(Collectors.toList()).get(0);
        if (valueList == null || valueList.size() < 1) {
            return resultBeanList;
        }

        MappingMessage<T> mappingMessage = getMappingMessage(tClass);
        BeanProperty<T> beanProperty = BeanHelper.getBeanProperty(tClass);
        for (List<Object> value : valueList) {
            resultBeanList.add(beanMapping(columns, value, tClass, mappingMessage, beanProperty));
        }
        return resultBeanList;
    }

    /**
     * 将Bean对象映射为influx表结构 插入时使用
     */
    public static <T> InsertMappingEntity<T> getInsertMapping(T t) throws Exception {
        Class<?> tClass = t.getClass();
        BeanProperty<T> beanProperty = BeanHelper.getBeanProperty(t);
        MappingMessage<T> mappingMessage = getMappingMessage(t);

        InsertMappingEntity<T> insertMappingEntity = new InsertMappingEntity<>();
        insertMappingEntity.setMeasurement(mappingMessage.getMeasurement());

        String timeFieldName = mappingMessage.getTimeFieldName();
        Class<?> timeType = mappingMessage.getTimeType();

        Map<String, Method> getMethod = beanProperty.getGetMethod();
        Method timeGetMethod = getMethod.get(timeFieldName);
        Object timeInvoke = timeGetMethod.invoke(t);
        long timestamp;

        if (Long.class.equals(timeType) || long.class.equals(timeType)) {
            timestamp = (long) timeInvoke;
        } else if (Date.class.equals(timeType)) {
            timestamp = ((Date) timeInvoke).getTime();
        } else if (LocalDateTime.class.equals(timeType)) {
            timestamp = TimeHelper.localToMills((LocalDateTime) timeInvoke);
        } else {
            throw new UnsupportedTimeException();
        }
        insertMappingEntity.setTime(timestamp);

        Map<String, String> tag = new HashMap<>();
        Map<String, Object> fields = new HashMap<>();
        List<TagAndFieldMapping> tagAndFieldMappingList = new ArrayList<>(mappingMessage.getTagAndFieldMappings().values());
        for (TagAndFieldMapping n : tagAndFieldMappingList) {
            Object invoke = getMethod.get(n.getBeanFieldName()).invoke(t);
            if (n.getTagFlag()) {
                tag.put(n.getColumn(), invoke.toString());
                continue;
            }
            fields.put(n.getColumn(), invoke);
        }

        return insertMappingEntity.setTag(tag).setFields(fields);

    }
}
