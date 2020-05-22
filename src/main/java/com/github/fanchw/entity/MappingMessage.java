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
public class MappingMessage<T> {
    private String measurement;

    private Class<?> timeType;

    private String timeFieldName;

    Map<String, TagAndFieldMapping> tagAndFieldMappings;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class TagAndFieldMapping {
        private String beanFieldName;

        private String column;

        private Class<?> beanFieldType;

        private Boolean tagFlag;
    }
}
