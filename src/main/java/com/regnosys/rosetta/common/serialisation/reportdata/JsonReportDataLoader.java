package com.regnosys.rosetta.common.serialisation.reportdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.reports.RegReportPaths;
import com.regnosys.rosetta.common.serialisation.AbstractJsonDataLoader;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JsonReportDataLoader extends AbstractJsonDataLoader<ReportDataSet> {

    public static final String DEFAULT_DESCRIPTOR_NAME = "regulatory-reporting-data-descriptor.json";

    public JsonReportDataLoader(ClassLoader classLoader,
                                ObjectMapper rosettaObjectMapper,
                                URL resourcesPath,
                                List<String> descriptorFileNames) {
        this(classLoader, rosettaObjectMapper, resourcesPath, descriptorFileNames, true);
    }

    public JsonReportDataLoader(ClassLoader classLoader,
                                ObjectMapper rosettaObjectMapper,
                                URL resourcesPath,
                                List<String> descriptorFileNames,
                                boolean loadInputFromFile) {
        this(classLoader, rosettaObjectMapper, resourcesPath, RegReportPaths.get(resourcesPath), descriptorFileNames, loadInputFromFile);
    }

    public JsonReportDataLoader(ClassLoader classLoader,
                                ObjectMapper rosettaObjectMapper,
                                URL resourcesPath,
                                RegReportPaths paths,
                                List<String> descriptorFileNames,
                                boolean loadInputFromFile) {
        super(classLoader, rosettaObjectMapper, resourcesPath, paths, descriptorFileNames, ReportDataSet.class, loadInputFromFile);
    }

    @Override
    protected ReportDataSet loadInputFiles(ReportDataSet descriptor) {
        List<ReportDataItem> loadedData = new ArrayList<>();
        for (ReportDataItem data : descriptor.getData()) {
            ReportDataItem reportDataItem = new ReportDataItem(data.getName(),
                    getInput(descriptor.getInputType(), data),
                    data.getExpected()); // expected is handled by JsonExpectedResultLoader
            loadedData.add(reportDataItem);
        }

        return new ReportDataSet(descriptor.getDataSetName(), descriptor.getInputType(), descriptor.getApplicableReports(), loadedData);
    }

    private Object getInput(String inputType, ReportDataItem data) {
        Class<?> inputTypeClass = loadClass(inputType, classLoader);
        if (data.getInput() instanceof String) {
            // by path
            String inputFileName = (String) data.getInput();
            Path inputFilePath = paths.getInputPath().resolve(inputFileName);
            return readType(inputTypeClass, rosettaObjectMapper, resolve(inputFilePath.toString()));
        } else {
            return fromObject(data.getInput(), inputTypeClass, rosettaObjectMapper);
        }
    }
}