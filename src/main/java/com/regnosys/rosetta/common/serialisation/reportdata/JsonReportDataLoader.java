package com.regnosys.rosetta.common.serialisation.reportdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.regnosys.rosetta.common.serialisation.AbstractJsonDataLoader;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JsonReportDataLoader extends AbstractJsonDataLoader<ReportDataSet> {

    public static final String DEFAULT_DESCRIPTOR_NAME = "regulatory-reporting-data-descriptor.json";

    public JsonReportDataLoader(ClassLoader classLoader,
                                ObjectMapper rosettaObjectMapper,
                                URI descriptorPath,
                                List<String> descriptorFileNames) {
        super(classLoader, rosettaObjectMapper, descriptorPath, descriptorFileNames, ReportDataSet.class, true);
    }

    @VisibleForTesting
    public JsonReportDataLoader(ClassLoader classLoader,
                                ObjectMapper rosettaObjectMapper,
                                URI descriptorPath,
                                List<String> descriptorFileNames,
                                boolean loadInputFromFile) {
        super(classLoader, rosettaObjectMapper, descriptorPath, descriptorFileNames, ReportDataSet.class, loadInputFromFile);
    }

    @Override
    protected ReportDataSet loadInputFiles(ReportDataSet descriptor) {
        List<ReportDataItem> loadedData = new ArrayList<>();
        for (ReportDataItem data : descriptor.getData()) {
            ReportDataItem reportDataItem = new ReportDataItem(data.getName(),
                    getInput(descriptor.getInputType(), data),
                    getExpected(descriptor.getExpectedType(), data));
            loadedData.add(reportDataItem);
        }

        return new ReportDataSet(descriptor.getDataSetName(), descriptor.getInputType(), descriptor.getApplicableReports(), loadedData);
    }

    private Object getInput(String inputType, ReportDataItem data) {
        Class<?> inputTypeClass = loadClass(inputType, classLoader);
        if (data.getInput() instanceof String) {
            return readType(inputTypeClass, rosettaObjectMapper, resolve((String) data.getInput()));
        } else {
            return fromObject(data.getInput(), inputTypeClass, rosettaObjectMapper);
        }
    }

    private Object getExpected(String expectedType, ReportDataItem data) {
        if (data.getExpected() == null){
            return null;
        }
        Class<?> expectedTypeClass = loadClass(expectedType, classLoader);
        if (data.getExpected() instanceof String) {
            return readType(expectedTypeClass, rosettaObjectMapper, resolve((String) data.getExpected()));
        } else {
            return fromObject(data.getExpected(), expectedTypeClass, rosettaObjectMapper);
        }
    }
}