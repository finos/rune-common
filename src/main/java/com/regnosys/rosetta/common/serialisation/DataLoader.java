package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;

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

    default <U> U readType(Class<U> type, ObjectMapper rosettaObjectMapper, URI uri) {
        try {
            return rosettaObjectMapper.readValue(uri.toURL(), type);
        } catch (IOException e) {
            throw new RuntimeException(uri + " cannot be serialised to " + type, e);
        }
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

    default <U> List<U> readTypeList(Class<U> type, ObjectMapper rosettaObjectMapper, InputStream inputStream) {
        try {
            return rosettaObjectMapper.readValue(inputStream, rosettaObjectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (IOException e) {
            throw new RuntimeException(inputStream + " cannot be serialised to list of " + type, e);
        }
    }

    default Class<?> loadClass(String type, ClassLoader classLoader) {
        try {
            return classLoader.loadClass(type);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load class for type " + type);
        }
    }

    default Optional<InputStream> openStream(URL descriptorUrl) {
        try {
            return Optional.of(descriptorUrl.openStream());
        } catch (FileNotFoundException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    default URL toURL(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
