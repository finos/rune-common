package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.util.UrlUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

public class JsonDataLoaderUtil {

    public static <U> U readType(Class<U> type, ObjectMapper rosettaObjectMapper, URL url) {
        try {
            return rosettaObjectMapper.readValue(UrlUtils.openURL(url), type);
        } catch (IOException e) {
            throw new RuntimeException(url + " cannot be serialised to " + type, e);
        }
    }

    public static <U> U readType(Class<U> type, ObjectMapper rosettaObjectMapper, String json) {
        try {
            return rosettaObjectMapper.readValue(json, type);
        } catch (IOException e) {
            throw new RuntimeException("JSON cannot be serialised to " + type + "[" + json + "]", e);
        }
    }

    public static <U> List<U> readTypeList(Class<U> type, ObjectMapper rosettaObjectMapper, URL url) {
        try {
            return rosettaObjectMapper.readValue(UrlUtils.openURL(url), rosettaObjectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (IOException e) {
            throw new RuntimeException(url + " cannot be serialised to list of " + type, e);
        }
    }

    public static <U> List<U> readTypeList(Class<U> type, ObjectMapper rosettaObjectMapper, Reader input) {
        try {
            return rosettaObjectMapper.readValue(input, rosettaObjectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (IOException e) {
            throw new RuntimeException(input + " cannot be serialised to list of " + type, e);
        }
    }

    public static <U> U fromObject(Object obj, Class<U> type, ObjectMapper rosettaObjectMapper) {
        try {
            return readType(type, rosettaObjectMapper, rosettaObjectMapper.writeValueAsString(obj));
        } catch (IOException e) {
            throw new RuntimeException(obj.getClass() + " cannot be serialised to " + type + "[" + obj.toString() + "]", e);
        }
    }

    public static Optional<Reader> openURL(URL descriptorUrl) {
        try {
            return Optional.of(UrlUtils.openURL(descriptorUrl));
        } catch (FileNotFoundException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Class<?> loadClass(String type, ClassLoader classLoader) {
        try {
            return classLoader.loadClass(type);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load class for type " + type);
        }
    }
}
