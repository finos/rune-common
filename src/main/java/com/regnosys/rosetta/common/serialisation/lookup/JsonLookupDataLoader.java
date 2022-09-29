package com.regnosys.rosetta.common.serialisation.lookup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.AbstractJsonDataLoader;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class JsonLookupDataLoader extends AbstractJsonDataLoader<LookupDataSet> {

    public static final String DEFAULT_DESCRIPTOR_NAME = "regulatory-reporting-lookup-descriptor.json";

    public JsonLookupDataLoader(ClassLoader classLoader,
                         ObjectMapper rosettaObjectMapper,
                         URL descriptorPath,
                         List<String> descriptorFileNames) {
        super(classLoader, rosettaObjectMapper, descriptorPath, descriptorFileNames, LookupDataSet.class, true);
    }

    public JsonLookupDataLoader(ClassLoader classLoader,
                                ObjectMapper rosettaObjectMapper,
                                URL descriptorPath,
                                List<String> descriptorFileNames, boolean loadInputFromFile) {
        super(classLoader, rosettaObjectMapper, descriptorPath, descriptorFileNames, LookupDataSet.class, loadInputFromFile);
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
            return readType(valueTypeClass, rosettaObjectMapper, resolve((String) data.getValue()));
        } else {
            return fromObject(data.getValue(), valueTypeClass, rosettaObjectMapper);
        }
    }

    private Object getKey(String keyType, LookupDataItem data) {
        Class<?> keyTypeClass = loadClass(keyType, classLoader);
        if (data.getKey().equals("*") || keyTypeClass == String.class) {
            return data.getKey();
        } else if (data.getKey() instanceof String) {
            return readType(keyTypeClass, rosettaObjectMapper, resolve((String) data.getKey()));
        } else {
            return fromObject(data.getKey(), keyTypeClass, rosettaObjectMapper);
        }
    }
}