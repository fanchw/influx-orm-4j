package com.github.fanchw.util;

import com.github.fanchw.entity.ConnectionMessage;
import com.github.fanchw.entity.InsertMappingEntity;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class InfluxOperationHelper {
    private static volatile InfluxDB influxDB = null;

    private static String DB;
    private static String retentionPolicy;

    public InfluxOperationHelper() {
    }

    public InfluxOperationHelper(ConnectionMessage connectionMessage) {
        this();
        this.connect(connectionMessage);
    }

    /**
     * 连接influx数据库
     */
    public void connect(ConnectionMessage connectionMessage) {
        if (influxDB == null) {
            synchronized (InfluxOperationHelper.class) {
                if (influxDB == null) {
                    influxDB = InfluxDBFactory.connect(connectionMessage.getUrl(),
                            connectionMessage.getUserName(), connectionMessage.getPassword());
                    try {
                        DB = connectionMessage.getDatabase();
                        retentionPolicy = connectionMessage.getRetentionPolicy();
                        creatDB(DB);
                    } catch (Exception e) {
                        log.warn("create influxDB failed,error:{}", e.getMessage());
                    } finally {
                        influxDB.setRetentionPolicy(connectionMessage.getRetentionPolicy());
                    }
                    influxDB.setLogLevel(InfluxDB.LogLevel.BASIC);
                }
            }
        }
    }


    /**
     * 创建数据库
     */
    private void creatDB(String database) {
        influxDB.query(new Query("CREATE DATABASE " + database, database));
    }

    public InfluxDB getInfluxDB() {
        return influxDB;
    }

    public QueryResult query(String sql) {
        return influxDB.query(new Query(sql, DB));
    }

    /**
     * 插入一条数据
     */
    public <T> boolean insertOne(T t) throws Exception {
        if (t == null) {
            log.error("NullPointer Value!");
            return false;
        }

        InsertMappingEntity<T> insertMapping = InfluxMappingHelper.getInsertMapping(t);
        Point point = Point.measurement(insertMapping.getMeasurement()).tag(insertMapping.getTag()).fields(insertMapping.getFields()).
                time(insertMapping.getTime(), TimeUnit.MILLISECONDS).build();
        influxDB.write(DB, retentionPolicy, point);
        return true;
    }

    /**
     * 批量插入数据
     */
    public <T> boolean batchInsert(List<T> dataList) throws Exception {
        if (dataList == null || dataList.size() < 1) {
            return false;
        }
        List<String> records = new ArrayList<>();
        for (T data : dataList) {
            InsertMappingEntity<T> insertMapping = InfluxMappingHelper.getInsertMapping(data);
            Point point = Point.measurement(insertMapping.getMeasurement()).tag(insertMapping.getTag()).fields(insertMapping.getFields()).
                    time(insertMapping.getTime(), TimeUnit.MILLISECONDS).build();
            String lineProtocol = BatchPoints.database(DB).retentionPolicy(retentionPolicy).point(point).build().lineProtocol();
            records.add(lineProtocol);
        }
        influxDB.write(DB, retentionPolicy, InfluxDB.ConsistencyLevel.ALL, records);
        return true;
    }

    /**
     * 根据类型进行分页查询
     */
    public <T> List<T> getResultLimit(Class<T> tClass, int page, int pageSize) throws Exception {
        String measurement = InfluxMappingHelper.getMeasurement(tClass);
        String sql = "select * from " + measurement + " limit " + page + " offset " + (page - 1) * pageSize;
        log.debug(sql);
        return InfluxMappingHelper.beanResult(query(sql), tClass);
    }

}
