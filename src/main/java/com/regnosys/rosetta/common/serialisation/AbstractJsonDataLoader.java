package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.util.UrlUtils;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.regnosys.rosetta.common.serialisation.JsonDataLoaderUtil.readTypeList;

public abstract class AbstractJsonDataLoader<T> implements DataLoader<T>, InputDataLoader<T> {

    protected final ClassLoader classLoader;
    protected final ObjectMapper rosettaObjectMapper;
    protected final URL descriptorPath;
    protected final Class<T> loadType;

    private final boolean loadInputFromFile;
    private final List<String> descriptorFileNames;

    protected AbstractJsonDataLoader(ClassLoader classLoader,
                                     ObjectMapper rosettaObjectMapper,
                                     URL descriptorPath,
                                     List<String> descriptorFileNames,
                                     Class<T> loadType,
                                     boolean loadInputFromFile) {
        this.classLoader = classLoader;
        this.rosettaObjectMapper = rosettaObjectMapper;
        this.descriptorPath = descriptorPath;
        this.descriptorFileNames = descriptorFileNames;
        this.loadType = loadType;
        this.loadInputFromFile = loadInputFromFile;
    }

    @Override
    public List<T> load() {
        return descriptorFileNames.stream()
                .map(fileName -> UrlUtils.resolve(descriptorPath, fileName))
                .map(JsonDataLoaderUtil::openURL)
                .filter(Optional::isPresent)
                .map(descriptorStream -> readTypeList(loadType, rosettaObjectMapper, descriptorStream.get()))
                .flatMap(Collection::stream)
                .map(i -> loadInputFromFile ? loadInputFiles(i) : i)
                .collect(Collectors.toList());
    }
}
