package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.reports.RegReportPaths;
import com.regnosys.rosetta.common.util.UrlUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractJsonDataLoader<T> implements DataLoader<T> {

    protected final ClassLoader classLoader;
    protected final ObjectMapper rosettaObjectMapper;
    protected final URL resourcesPath;
    protected final RegReportPaths paths;
    protected final Class<T> loadType;

    private final boolean loadInputFromFile;
    private final List<String> descriptorFileNames;

    protected AbstractJsonDataLoader(ClassLoader classLoader,
                                     ObjectMapper rosettaObjectMapper,
                                     URL resourcesPath,
                                     RegReportPaths paths,
                                     List<String> descriptorFileNames,
                                     Class<T> loadType,
                                     boolean loadInputFromFile) {
        this.classLoader = classLoader;
        this.rosettaObjectMapper = rosettaObjectMapper;
        this.resourcesPath = resourcesPath;
        this.paths = paths;
        this.descriptorFileNames = descriptorFileNames;
        this.loadType = loadType;
        this.loadInputFromFile = loadInputFromFile;
    }

    @Override
    public List<T> load() {
        return descriptorFileNames.stream()
                .map(paths::getDescriptorPath)
                .map(Path::toString)
                .map(this::resolve)
                .map(this::openURL)
                .filter(Optional::isPresent)
                .map(descriptorStream -> readTypeList(loadType, rosettaObjectMapper, descriptorStream.get()))
                .flatMap(Collection::stream)
                .map(i -> loadInputFromFile ? loadInputFiles(i) : i)
                .collect(Collectors.toList());
    }

    public URL resolve(String d) {
    	return UrlUtils.resolve(resourcesPath, d);
    }

    protected abstract T loadInputFiles(T descriptor);

    protected <U> U fromObject(Object obj, Class<U> type, ObjectMapper rosettaObjectMapper) {
        try {
            return readType(type, rosettaObjectMapper, rosettaObjectMapper.writeValueAsString(obj));
        } catch (IOException e) {
            throw new RuntimeException(obj.getClass() + " cannot be serialised to " + type + "[" + obj.toString() + "]", e);
        }
    }

    protected <U> U readType(Class<U> type, ObjectMapper rosettaObjectMapper, URL url) {
        try {
            return rosettaObjectMapper.readValue(UrlUtils.openURL(url), type);
        } catch (IOException e) {
            throw new RuntimeException(url + " cannot be serialised to " + type, e);
        }
    }

    protected Optional<Reader> openURL(URL descriptorUrl) {
        try {
            return Optional.of(UrlUtils.openURL(descriptorUrl));
        } catch (FileNotFoundException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private <U> U readType(Class<U> type, ObjectMapper rosettaObjectMapper, String json) {
        try {
            return rosettaObjectMapper.readValue(json, type);
        } catch (IOException e) {
            throw new RuntimeException("JSON cannot be serialised to " + type + "[" + json + "]", e);
        }
    }

    protected <U> List<U> readTypeList(Class<U> type, ObjectMapper rosettaObjectMapper, URL url) {
        try {
            return rosettaObjectMapper.readValue(UrlUtils.openURL(url), rosettaObjectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (IOException e) {
            throw new RuntimeException(url + " cannot be serialised to list of " + type, e);
        }
    }

    private  <U> List<U> readTypeList(Class<U> type, ObjectMapper rosettaObjectMapper, Reader input) {
        try {
            return rosettaObjectMapper.readValue(input, rosettaObjectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (IOException e) {
            throw new RuntimeException(input + " cannot be serialised to list of " + type, e);
        }
    }

    protected Class<?> loadClass(String type, ClassLoader classLoader) {
        try {
            return classLoader.loadClass(type);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load class for type " + type);
        }
    }
}
