package com.regnosys.rosetta.common.serialisation.reportdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.DataLoader;

import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JsonReportDataLoader implements DataLoader<ReportDataSet> {

    public static final String DEFAULT_DESCRIPTOR_NAME = "regulatory-reporting-data-descriptor.json";
    private final ClassLoader classLoader;
    private final ObjectMapper rosettaObjectMapper;
    private final URI descriptorPath;
    private final String descriptorFileName;

    JsonReportDataLoader(ClassLoader classLoader,
                         ObjectMapper rosettaObjectMapper,
                         URI descriptorPath,
                         String descriptorFileName) {
        this.classLoader = classLoader;
        this.rosettaObjectMapper = rosettaObjectMapper;
        this.descriptorPath = descriptorPath;
        this.descriptorFileName = descriptorFileName;
    }

    public JsonReportDataLoader(ClassLoader classLoader, ObjectMapper rosettaObjectMapper, URI descriptorPath) {
        this(classLoader, rosettaObjectMapper, descriptorPath, DEFAULT_DESCRIPTOR_NAME);
    }

    @Override
    public List<ReportDataSet> load() {
        Optional<InputStream> descriptorStream = openStream(toURL(descriptorPath.resolve(descriptorFileName)));
        if (!descriptorStream.isPresent()) {
            return Collections.emptyList();
        }
        List<ReportDataSet> reportDataSets = readTypeList(ReportDataSet.class, rosettaObjectMapper, descriptorStream.get());
        return reportDataSets.stream().map(this::loadInputFiles).collect(Collectors.toList());
    }

    private ReportDataSet loadInputFiles(ReportDataSet descriptor) {
        List<ReportDataItem> loadedData = descriptor.getData().stream().map(data -> new ReportDataItem(data.getName(), getInput(descriptor.getInputType(), data)))
                .collect(Collectors.toList());

        return new ReportDataSet(descriptor.getDataSetName(), descriptor.getInputType(), descriptor.getApplicableReports(), loadedData);
    }

    private Object getInput(String inputType, ReportDataItem data) {
        Class<?> inputTypeClass = loadClass(inputType, classLoader);

        if (data.getInput() instanceof String) {
            return readType(inputTypeClass, rosettaObjectMapper, descriptorPath.resolve((String) data.getInput()));
        } else {
            return fromObject(data.getInput(), inputTypeClass, rosettaObjectMapper);
        }
    }
}