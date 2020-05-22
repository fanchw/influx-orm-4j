package com.github.fanchw.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class TimeHelper {
    public static final String INFLUX_DATE_FORMAT = "yyyy-MM-dd/HH:mm:ss";
    public static final SimpleDateFormat INFLUX_SDF = new SimpleDateFormat(INFLUX_DATE_FORMAT);
    public static final String INFLUX_T = "T";
    public static final String INFLUX_Z = "Z";
    public static final String JAVA_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final SimpleDateFormat JAVA_SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final String BEIJING = "+8";
    public static final long EIGHT_HOUR = 1000L * 60L * 60L * 8L;

    public static String dateToInfluxString(Date date) {
        String format = INFLUX_SDF.format(new Date(date.getTime() - EIGHT_HOUR));
        String[] split = format.split("/");
        return split[0] + INFLUX_T + split[1] + INFLUX_Z;
    }

    public static Date influxDateToJavaDate(String influxDate) {
        String[] ts = influxDate.split(INFLUX_T);
        String javaDateStr = (ts[0] + " " + ts[1]).split(INFLUX_Z)[0];
        Date result = null;
        try {
            result = new Date(JAVA_SDF.parse(javaDateStr).getTime() + EIGHT_HOUR);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static long localToMills(LocalDateTime localDateTime) {
        return localDateTime.toInstant(ZoneOffset.of(BEIJING)).toEpochMilli();
    }

    public static LocalDateTime millsToLocal(long mills) {
        return Instant.ofEpochMilli(mills).atOffset(ZoneOffset.of(BEIJING)).toLocalDateTime();
    }
}
