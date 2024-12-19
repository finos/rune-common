package com.regnosys.rosetta.common.serialisation.lookup;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
 * ==============
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.AbstractJsonDataLoader;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import static com.regnosys.rosetta.common.serialisation.JsonDataLoaderUtil.*;

@Deprecated
public class JsonLookupDataLoader extends AbstractJsonDataLoader<LookupDataSet> {

    public static final String DEFAULT_DESCRIPTOR_NAME = "regulatory-reporting-lookup-descriptor.json";

    private final URL inputPath;

    public JsonLookupDataLoader(ClassLoader classLoader,
                                ObjectMapper rosettaObjectMapper,
                                URL descriptorPath,
                                List<String> descriptorFileNames) {
        super(classLoader, rosettaObjectMapper, descriptorPath, descriptorFileNames, LookupDataSet.class, false);
        this.inputPath = null;
    }

    public JsonLookupDataLoader(ClassLoader classLoader,
                                ObjectMapper rosettaObjectMapper,
                                URL descriptorPath,
                                List<String> descriptorFileNames,
                                URL inputPath) {
        super(classLoader, rosettaObjectMapper, descriptorPath, descriptorFileNames, LookupDataSet.class, true);
        this.inputPath = inputPath;
    }

    @Override
    public LookupDataSet loadInputFiles(LookupDataSet descriptor) {
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
            String valuePath = (String) data.getValue();
            return readType(valueTypeClass, rosettaObjectMapper, resolve(inputPath, valuePath));
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
            return readType(keyTypeClass, rosettaObjectMapper, resolve(inputPath, keyPath));
        } else {
            return fromObject(data.getKey(), keyTypeClass, rosettaObjectMapper);
        }
    }
}
