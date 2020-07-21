package com.regnosys.rosetta.common.serialisation.reportdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.DataLoader;
import com.regnosys.rosetta.common.util.ClassPathUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

public class JsonReportDataLoader implements DataLoader<ReportDataSet> {

    public static final String DEFAULT_DESCRIPTOR_NAME = "regulatory-reporting-data-descriptor.json";
    private final ClassLoader classLoader;
    private final ObjectMapper rosettaObjectMapper;
    private final String descriptorPath;
    private final String descriptorFileName;

    JsonReportDataLoader(ClassLoader classLoader,
                         ObjectMapper rosettaObjectMapper,
                         String descriptorPath,
                         String descriptorFileName) {
        this.classLoader = classLoader;
        this.rosettaObjectMapper = rosettaObjectMapper;
        this.descriptorPath = descriptorPath;
        this.descriptorFileName = descriptorFileName;
    }

    public JsonReportDataLoader(ClassLoader classLoader, ObjectMapper rosettaObjectMapper, String descriptorPath) {
        this(classLoader, rosettaObjectMapper, descriptorPath, DEFAULT_DESCRIPTOR_NAME);
    }

    @Override
    public List<ReportDataSet> load() {
        return ClassPathUtils.findPathsFromClassPath(singletonList(descriptorPath), ".*" + descriptorFileName, Optional.empty(), classLoader)
                .stream()
                .map(ClassPathUtils::toUrl)
                .map(url -> readTypeList(ReportDataSet.class, rosettaObjectMapper, url))
                .flatMap(Collection::stream)
                .map(this::loadInputFiles)
                .collect(Collectors.toList());
    }

    private ReportDataSet loadInputFiles(ReportDataSet descriptor) {
        List<ReportDataItem> loadedData = descriptor.getData().stream().map(data -> new ReportDataItem(data.getName(), getInput(descriptor.getInputType(), data)))
                .collect(Collectors.toList());

        return new ReportDataSet(descriptor.getDataSetName(), descriptor.getInputType(), descriptor.getApplicableReports(), loadedData);
    }

    private Object getInput(String inputType, ReportDataItem data) {
        Class<?> inputTypeClass = loadClass(inputType, classLoader);

        if (data.getInput() instanceof String) {
            return fromClasspath(descriptorPath + "/" + data.getInput(), inputTypeClass, rosettaObjectMapper, classLoader);
        } else {
            return fromObject(data.getInput(), inputTypeClass, rosettaObjectMapper);
        }
    }
}