package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.util.ClassPathUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Interface to lookup model related data from an external source. The data is typically a model instance that can be
 * loaded from a json source (e.g. file, rest api).
 */
public interface DataLoader<T> {
    List<T> load();

    default <U> U fromObject(Object obj, Class<U> type, ObjectMapper rosettaObjectMapper) {
        try {
            return readType(type, rosettaObjectMapper, rosettaObjectMapper.writeValueAsString(obj));
        } catch (IOException e) {
            throw new RuntimeException(obj.getClass() + " cannot be serialised to " + type + "[" + obj.toString() + "]", e);
        }
    }

    default <U> U fromClasspath(String filePath, Class<U> type, ObjectMapper rosettaObjectMapper, ClassLoader classLoader) {
        return ClassPathUtils.loadFromClasspath(filePath, classLoader).findFirst()
                .map(ClassPathUtils::toUrl)
                .map(url -> readType(type, rosettaObjectMapper, url))
                .orElseThrow(() -> new RuntimeException("Could not load " + filePath + " of type " + type));
    }

    default <U> U readType(Class<U> type, ObjectMapper rosettaObjectMapper, URL url) {
        try {
            return rosettaObjectMapper.readValue(url, type);
        } catch (IOException e) {
            throw new RuntimeException(url + " cannot be serialised to " + type, e);
        }
    }


    default <U> U readType(Class<U> type, ObjectMapper rosettaObjectMapper, String json) {
        try {
            return rosettaObjectMapper.readValue(json, type);
        } catch (IOException e) {
            throw new RuntimeException("JSON cannot be serialised to " + type + "[" + json + "]", e);
        }
    }

    default <U> List<U> readTypeList(Class<U> type, ObjectMapper rosettaObjectMapper, URL url) {
        try {
            return rosettaObjectMapper.readValue(url, rosettaObjectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (IOException e) {
            throw new RuntimeException(url + " cannot be serialised to list of " + type, e);
        }
    }

    default Class<?> loadClass(String type, ClassLoader classLoader) {
        try {
            return classLoader.loadClass(type);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load class for type " + type);
        }
    }

}
