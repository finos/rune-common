package com.regnosys.rosetta.common.serialisation;

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
import com.regnosys.rosetta.common.serialisation.reportdata.ReportDataItem;
import com.regnosys.rosetta.common.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.regnosys.rosetta.common.serialisation.JsonDataLoaderUtil.*;

@Deprecated
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

    public List<ReportDataItem> getDataItem(DataSet descriptor, URL inputPath){
        List<ReportDataItem> loadedData = new ArrayList<>();
        for (ReportDataItem data : descriptor.getData()) {
            ReportDataItem reportDataItem;
            try {
                reportDataItem = new ReportDataItem(data.getName(), getInput(descriptor.getInputType(), data, inputPath),
                        data.getExpected()); // expected is handled by JsonExpectedResultLoader
            } catch (RuntimeException e) {
                reportDataItem = new ReportDataItem(data.getName(), data.getInput(), data.getExpected(), e);
            }
            loadedData.add(reportDataItem);
        }
        return loadedData;
    }
}
