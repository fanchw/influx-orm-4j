package com.github.fanchw;

import com.github.fanchw.domain.SoftData;
import com.github.fanchw.entity.ConnectionMessage;
import com.github.fanchw.util.InfluxOperationHelper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class BasicTest {
    ConnectionMessage connectionMessage = new ConnectionMessage("test", "123456",
            "http://127.0.0.1:8086", "test", "autogen");
    InfluxOperationHelper influxOperationHelper = new InfluxOperationHelper(connectionMessage);

    SoftData softData = new SoftData("NH3", "WARN", 53.1, System.currentTimeMillis());

    @Test
    public void conn() throws Exception {
        influxOperationHelper.insertOne(softData);
    }

    @Test
    public void simpleQuery() throws Exception {
        System.out.println(influxOperationHelper.getResultLimit(SoftData.class, 1, 1));
    }
}
