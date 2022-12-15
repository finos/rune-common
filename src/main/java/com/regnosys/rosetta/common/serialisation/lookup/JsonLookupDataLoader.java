package com.regnosys.rosetta.common.serialisation.lookup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.reports.RegReportPaths;
import com.regnosys.rosetta.common.serialisation.AbstractJsonDataLoader;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class JsonLookupDataLoader extends AbstractJsonDataLoader<LookupDataSet> {

    public static final String DEFAULT_DESCRIPTOR_NAME = "regulatory-reporting-lookup-descriptor.json";

    public JsonLookupDataLoader(ClassLoader classLoader,
                         ObjectMapper rosettaObjectMapper,
                         URL resourcesPath,
                         List<String> descriptorFileNames) {
        this(classLoader, rosettaObjectMapper, resourcesPath, descriptorFileNames, true);
    }

    public JsonLookupDataLoader(ClassLoader classLoader,
                                ObjectMapper rosettaObjectMapper,
                                URL resourcesPath,
                                List<String> descriptorFileNames,
                                boolean loadInputFromFile) {
        super(classLoader, rosettaObjectMapper, resourcesPath, RegReportPaths.get(resourcesPath), descriptorFileNames, LookupDataSet.class, loadInputFromFile);
    }

    public JsonLookupDataLoader(ClassLoader classLoader,
                                ObjectMapper rosettaObjectMapper,
                                URL resourcesPath,
                                RegReportPaths paths,
                                List<String> descriptorFileNames,
                                boolean loadInputFromFile) {
        super(classLoader, rosettaObjectMapper, resourcesPath, paths, descriptorFileNames, LookupDataSet.class, loadInputFromFile);
    }

    @Override
    protected LookupDataSet loadInputFiles(LookupDataSet descriptor) {
        List<LookupDataItem> loadedData = descriptor.getData().stream()
                .map(data -> new LookupDataItem(
                        getKey(descriptor.getKeyType(), data),
                        getValue(descriptor.getValueType(), data)))
                .collect(Collectors.toList());
        return new LookupDataSet(descriptor.getName(), descriptor.getKeyType(), descriptor.getValueType(), loadedData);
    }

    private Object getValue(String valueType, LookupDataItem data) {
        Class<?> valueTypeClass = loadClass(valueType, classLoader);
        if (data.getValue() instanceof String) {
            String inputFileName = (String) data.getValue();
            Path inputFilePath = paths.getInputPath().resolve(inputFileName);
            return readType(valueTypeClass, rosettaObjectMapper, resolve(inputFilePath.toString()));
        } else {
            return fromObject(data.getValue(), valueTypeClass, rosettaObjectMapper);
        }
    }

    private Object getKey(String keyType, LookupDataItem data) {
        Class<?> keyTypeClass = loadClass(keyType, classLoader);
        if (data.getKey().equals("*") || keyTypeClass == String.class) {
            return data.getKey();
        } else if (data.getKey() instanceof String) {
            String keyPath = (String) data.getKey();
            return readType(keyTypeClass, rosettaObjectMapper, resolve(keyPath));
        } else {
            return fromObject(data.getKey(), keyTypeClass, rosettaObjectMapper);
        }
    }
}