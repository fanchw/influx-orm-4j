package com.github.fanchw.domain;

import com.github.fanchw.annotation.InfluxFiled;
import com.github.fanchw.annotation.InfluxMeasurement;
import com.github.fanchw.annotation.InfluxTag;
import com.github.fanchw.annotation.InfluxTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@InfluxMeasurement("softData")
public class SoftData {

    @InfluxTag
    private String tagName;

    @InfluxTag
    private String tagType;

    @InfluxFiled
    private Double value;

    @InfluxTime
    private Long time;
}
