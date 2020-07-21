package com.regnosys.rosetta.common.serialisation.lookup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.DataLoader;
import com.regnosys.rosetta.common.util.ClassPathUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

public class JsonLookupDataLoader implements DataLoader<LookupDataSet> {

    public static final String DEFAULT_DESCRIPTOR_NAME = "regulatory-reporting-lookup-descriptor.json";
    private final ClassLoader classLoader;
    private final ObjectMapper rosettaObjectMapper;
    private final String descriptorPath;
    private final String descriptorFileName;

    JsonLookupDataLoader(ClassLoader classLoader,
                         ObjectMapper rosettaObjectMapper,
                         String descriptorPath,
                         String descriptorFileName) {
        this.classLoader = classLoader;
        this.rosettaObjectMapper = rosettaObjectMapper;
        this.descriptorPath = descriptorPath;
        this.descriptorFileName = descriptorFileName;
    }

    public JsonLookupDataLoader(ClassLoader classLoader, ObjectMapper rosettaObjectMapper, String descriptorPath) {
        this(classLoader, rosettaObjectMapper, descriptorPath, DEFAULT_DESCRIPTOR_NAME);
    }

    @Override
    public List<LookupDataSet> load() {

        List<LookupDataSet> collect = ClassPathUtils.findPathsFromClassPath(singletonList(descriptorPath), ".*" + descriptorFileName, Optional.empty(), classLoader)
                .stream()
                .map(ClassPathUtils::toUrl)
                .map(url -> readTypeList(LookupDataSet.class, rosettaObjectMapper, url))
                .flatMap(x -> x.stream())
                .collect(Collectors.toList());

        return ClassPathUtils.findPathsFromClassPath(singletonList(descriptorPath), ".*" + descriptorFileName, Optional.empty(), classLoader)
                .stream()
                .map(ClassPathUtils::toUrl)
                .map(url -> readTypeList(LookupDataSet.class, rosettaObjectMapper, url))
                .flatMap(x -> x.stream())
                .map(x -> loadInputFiles(x))
                .collect(Collectors.toList());
    }

    private LookupDataSet loadInputFiles(LookupDataSet descriptor) {
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
            return fromClasspath(descriptorPath + "/" + data.getValue(), valueTypeClass, rosettaObjectMapper, classLoader);
        } else {
            return fromObject(data.getValue(), valueTypeClass, rosettaObjectMapper);
        }
    }

    private Object getKey(String keyType, LookupDataItem data) {
        Class<?> keyTypeClass = loadClass(keyType, classLoader);
        if (data.getKey().equals("*") || keyTypeClass == String.class) {
            return data.getKey();
        } else if (data.getKey() instanceof String) {
            return fromClasspath(descriptorPath + "/" +  data.getKey(), keyTypeClass, rosettaObjectMapper, classLoader);
        } else {
            return fromObject(data.getKey(), keyTypeClass, rosettaObjectMapper);
        }
    }
}