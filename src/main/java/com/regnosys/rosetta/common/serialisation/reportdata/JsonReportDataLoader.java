package com.regnosys.rosetta.common.serialisation.reportdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.AbstractJsonDataLoader;
import com.regnosys.rosetta.common.serialisation.DataSet;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.regnosys.rosetta.common.serialisation.JsonDataLoaderUtil.*;

public class JsonReportDataLoader extends AbstractJsonDataLoader<DataSet> {

    public static final String DEFAULT_DESCRIPTOR_NAME = "regulatory-reporting-data-descriptor.json";

    private final URL inputPath;

    public JsonReportDataLoader(ClassLoader classLoader,
                                ObjectMapper rosettaObjectMapper,
                                URL descriptorPath,
                                List<String> descriptorFileNames) {
        super(classLoader, rosettaObjectMapper, descriptorPath, descriptorFileNames, DataSet.class, false);
        this.inputPath = null;
    }

    public JsonReportDataLoader(ClassLoader classLoader,
                                ObjectMapper rosettaObjectMapper,
                                URL descriptorPath,
                                List<String> descriptorFileNames,
                                URL inputPath) {
        super(classLoader, rosettaObjectMapper, descriptorPath, descriptorFileNames, DataSet.class, true);
        this.inputPath = inputPath;
    }

    @Override
    public DataSet loadInputFiles(DataSet descriptor) {
        List<ReportDataItem> loadedData = getDataItem(descriptor, inputPath);
        return new DataSet(descriptor.getDataSetName(), descriptor.getInputType(), loadedData, descriptor.getApplicableReports(), descriptor.getApplicableProjections());
    }
}