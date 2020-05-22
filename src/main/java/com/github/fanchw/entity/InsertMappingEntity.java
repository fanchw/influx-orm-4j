package com.github.fanchw.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class InsertMappingEntity<T> {
    private String measurement;

    private Map<String, String> tag;

    private Map<String, Object> fields;

    private long time;
}
