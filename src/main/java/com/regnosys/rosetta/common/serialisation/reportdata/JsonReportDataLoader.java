package com.regnosys.rosetta.common.serialisation.reportdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.AbstractJsonDataLoader;
import com.regnosys.rosetta.common.util.UrlUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.regnosys.rosetta.common.serialisation.JsonDataLoaderUtil.*;

public class JsonReportDataLoader extends AbstractJsonDataLoader<ReportDataSet> {

    public static final String DEFAULT_DESCRIPTOR_NAME = "regulatory-reporting-data-descriptor.json";

    private final URL inputPath;

    public JsonReportDataLoader(ClassLoader classLoader,
                                ObjectMapper rosettaObjectMapper,
                                URL descriptorPath,
                                List<String> descriptorFileNames) {
        super(classLoader, rosettaObjectMapper, descriptorPath, descriptorFileNames, ReportDataSet.class, false);
        this.inputPath = null;
    }

    public JsonReportDataLoader(ClassLoader classLoader,
                                ObjectMapper rosettaObjectMapper,
                                URL descriptorPath,
                                List<String> descriptorFileNames,
                                URL inputPath) {
        super(classLoader, rosettaObjectMapper, descriptorPath, descriptorFileNames, ReportDataSet.class, true);
        this.inputPath = inputPath;
    }

    @Override
    public ReportDataSet loadInputFiles(ReportDataSet descriptor) {
        List<ReportDataItem> loadedData = new ArrayList<>();
        for (ReportDataItem data : descriptor.getData()) {
            ReportDataItem reportDataItem;
            try {
                reportDataItem = new ReportDataItem(data.getName(), getInput(descriptor.getInputType(), data),
                        data.getExpected()); // expected is handled by JsonExpectedResultLoader
            } catch (RuntimeException e) {
                reportDataItem = new ReportDataItem(data.getName(), data.getInput(), data.getExpected(), e);
            }
            loadedData.add(reportDataItem);
        }
        return new ReportDataSet(descriptor.getDataSetName(), descriptor.getInputType(), descriptor.getApplicableReports(), loadedData);
    }

    private Object getInput(String inputType, ReportDataItem data) {
        Class<?> inputTypeClass = loadClass(inputType, classLoader);
        if (data.getInput() instanceof String) {
            // by path
            String inputFileName = (String) data.getInput();
            return readType(inputTypeClass, rosettaObjectMapper, resolve(inputPath, inputFileName));
        } else {
            return fromObject(data.getInput(), inputTypeClass, rosettaObjectMapper);
        }
    }
}