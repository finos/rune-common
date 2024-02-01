package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.reportdata.ReportDataItem;
import com.regnosys.rosetta.common.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.regnosys.rosetta.common.serialisation.JsonDataLoaderUtil.*;
import static com.regnosys.rosetta.common.serialisation.JsonDataLoaderUtil.fromObject;

public abstract class AbstractJsonDataLoader<T> implements DataLoader<T>, InputDataLoader<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJsonDataLoader.class);

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
                .map(fileName -> resolve(descriptorPath, fileName))
                .map(JsonDataLoaderUtil::openURL)
                .filter(Optional::isPresent)
                .map(descriptorStream -> readTypeList(loadType, rosettaObjectMapper, descriptorStream.get()))
                .flatMap(Collection::stream)
                .map(i -> loadInputFromFile ? loadInputFiles(i) : i)
                .collect(Collectors.toList());
    }

    // This can be overridden is downstream projects
    public URL resolve(URL url, String child) {
        URL resolvedUrl = UrlUtils.resolve(url, child);
        LOGGER.debug("Resolved URL {}", resolvedUrl);
        return resolvedUrl;
    }


    public Object getInput(String inputType, ReportDataItem data, URL inputPath) {
        Class<?> inputTypeClass = loadClass(inputType, classLoader);
        if (data.getInput() instanceof String) {
            // by path
            String inputFileName = (String) data.getInput();
            return readType(inputTypeClass, rosettaObjectMapper, resolve(inputPath, inputFileName));
        } else {
            return fromObject(data.getInput(), inputTypeClass, rosettaObjectMapper);
        }
    }
}
