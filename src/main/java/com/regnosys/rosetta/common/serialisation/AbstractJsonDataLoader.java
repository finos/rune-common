package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractJsonDataLoader<T> implements DataLoader<T> {

    protected final ClassLoader classLoader;
    protected final ObjectMapper rosettaObjectMapper;
    protected final URI descriptorPath;
    protected final List<String> descriptorFileNames;
    protected final Class<T> loadType;

    protected AbstractJsonDataLoader(ClassLoader classLoader,
                                     ObjectMapper rosettaObjectMapper,
                                     URI descriptorPath,
                                     List<String> descriptorFileNames,
                                     Class<T> loadType) {
        this.classLoader = classLoader;
        this.rosettaObjectMapper = rosettaObjectMapper;
        this.descriptorPath = descriptorPath;
        this.descriptorFileNames = descriptorFileNames;
        this.loadType = loadType;
    }

    @Override
    public List<T> load() {
        return descriptorFileNames.stream()
                .map(descriptorPath::resolve)
                .map(this::toURL)
                .map(this::openStream)
                .filter(Optional::isPresent)
                .map(descriptorStream -> readTypeList(loadType, rosettaObjectMapper, descriptorStream.get()))
                .flatMap(Collection::stream)
                .map(this::loadInputFiles)
                .collect(Collectors.toList());
    }

    protected abstract T loadInputFiles(T descriptor);

    protected <U> U fromObject(Object obj, Class<U> type, ObjectMapper rosettaObjectMapper) {
        try {
            return readType(type, rosettaObjectMapper, rosettaObjectMapper.writeValueAsString(obj));
        } catch (IOException e) {
            throw new RuntimeException(obj.getClass() + " cannot be serialised to " + type + "[" + obj.toString() + "]", e);
        }
    }

    protected <U> U readType(Class<U> type, ObjectMapper rosettaObjectMapper, URI uri) {
        try {
            return rosettaObjectMapper.readValue(uri.toURL(), type);
        } catch (IOException e) {
            throw new RuntimeException(uri + " cannot be serialised to " + type, e);
        }
    }

    protected <U> U readType(Class<U> type, ObjectMapper rosettaObjectMapper, URL url) {
        try {
            return rosettaObjectMapper.readValue(url, type);
        } catch (IOException e) {
            throw new RuntimeException(url + " cannot be serialised to " + type, e);
        }
    }


    protected <U> U readType(Class<U> type, ObjectMapper rosettaObjectMapper, String json) {
        try {
            return rosettaObjectMapper.readValue(json, type);
        } catch (IOException e) {
            throw new RuntimeException("JSON cannot be serialised to " + type + "[" + json + "]", e);
        }
    }

    protected <U> List<U> readTypeList(Class<U> type, ObjectMapper rosettaObjectMapper, InputStream inputStream) {
        try {
            return rosettaObjectMapper.readValue(inputStream, rosettaObjectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (IOException e) {
            throw new RuntimeException(inputStream + " cannot be serialised to list of " + type, e);
        }
    }

    protected Class<?> loadClass(String type, ClassLoader classLoader) {
        try {
            return classLoader.loadClass(type);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load class for type " + type);
        }
    }

    protected Optional<InputStream> openStream(URL descriptorUrl) {
        try {
            return Optional.of(descriptorUrl.openStream());
        } catch (FileNotFoundException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected URL toURL(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
