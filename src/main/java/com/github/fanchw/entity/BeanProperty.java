package com.github.fanchw.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class BeanProperty<T> {

    private List<String> fieldName;

    private Map<String, Method> getMethod;

    private Map<String, Method> setMethod;
}
