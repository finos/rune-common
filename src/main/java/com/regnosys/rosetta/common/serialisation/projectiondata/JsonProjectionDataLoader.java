package com.regnosys.rosetta.common.serialisation.projectiondata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.AbstractJsonDataLoader;
import com.regnosys.rosetta.common.serialisation.DataSet;
import com.regnosys.rosetta.common.serialisation.reportdata.ReportDataItem;

import java.net.URL;
import java.util.List;

public class JsonProjectionDataLoader extends AbstractJsonDataLoader<DataSet> {

    public static final String DEFAULT_DESCRIPTOR_NAME = "data-descriptor.json";

    private final URL inputPath;

    public JsonProjectionDataLoader(ClassLoader classLoader,
                                    ObjectMapper rosettaObjectMapper,
                                    URL descriptorPath,
                                    List<String> descriptorFileNames) {
        super(classLoader, rosettaObjectMapper, descriptorPath, descriptorFileNames, DataSet.class, false);
        this.inputPath = null;
    }

    public JsonProjectionDataLoader(ClassLoader classLoader,
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
        return new DataSet(descriptor.getDataSetName(), descriptor.getDataSetShortName(), descriptor.getInputType(), loadedData, descriptor.getApplicableReports(), descriptor.getApplicableProjections());
    }
}