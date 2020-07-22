package com.regnosys.rosetta.common.serialisation.lookup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.DataLoader;

import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JsonLookupDataLoader implements DataLoader<LookupDataSet> {

    public static final String DEFAULT_DESCRIPTOR_NAME = "regulatory-reporting-lookup-descriptor.json";
    private final ClassLoader classLoader;
    private final ObjectMapper rosettaObjectMapper;
    private final URI descriptorPath;
    private final String descriptorFileName;

    JsonLookupDataLoader(ClassLoader classLoader,
                         ObjectMapper rosettaObjectMapper,
                         URI descriptorPath,
                         String descriptorFileName) {
        this.classLoader = classLoader;
        this.rosettaObjectMapper = rosettaObjectMapper;
        this.descriptorPath = descriptorPath;
        this.descriptorFileName = descriptorFileName;
    }

    public JsonLookupDataLoader(ClassLoader classLoader, ObjectMapper rosettaObjectMapper, URI descriptorPath) {
        this(classLoader, rosettaObjectMapper, descriptorPath, DEFAULT_DESCRIPTOR_NAME);
    }

    @Override
    public List<LookupDataSet> load() {
        URI uri = descriptorPath.resolve(descriptorFileName);
        Optional<InputStream> descriptorStream = openStream(toURL(uri));
        if (!descriptorStream.isPresent()) {
            return Collections.emptyList();
        }

        List<LookupDataSet> lookupDataSets = readTypeList(LookupDataSet.class, rosettaObjectMapper, descriptorStream.get());
        return lookupDataSets.stream().map(this::loadInputFiles).collect(Collectors.toList());
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
            return readType(valueTypeClass, rosettaObjectMapper, descriptorPath.resolve((String) data.getValue()));
        } else {
            return fromObject(data.getValue(), valueTypeClass, rosettaObjectMapper);
        }
    }

    private Object getKey(String keyType, LookupDataItem data) {
        Class<?> keyTypeClass = loadClass(keyType, classLoader);
        if (data.getKey().equals("*") || keyTypeClass == String.class) {
            return data.getKey();
        } else if (data.getKey() instanceof String) {
            return readType(keyTypeClass, rosettaObjectMapper, descriptorPath.resolve((String) data.getKey()));
        } else {
            return fromObject(data.getKey(), keyTypeClass, rosettaObjectMapper);
        }
    }
}