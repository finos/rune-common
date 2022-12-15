package com.regnosys.rosetta.common.serialisation.reportdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.reports.RegReportIdentifier;
import com.regnosys.rosetta.common.reports.RegReportPaths;
import com.regnosys.rosetta.common.serialisation.AbstractJsonDataLoader;
import com.regnosys.rosetta.common.util.UrlUtils;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JsonExpectedResultLoader extends AbstractJsonDataLoader<ReportIdentifierDataSet> {

    public JsonExpectedResultLoader(ClassLoader classLoader, ObjectMapper rosettaObjectMapper, URL resourcesPath) {
        this(classLoader, rosettaObjectMapper, resourcesPath, RegReportPaths.get(resourcesPath));
    }

    public JsonExpectedResultLoader(ClassLoader classLoader, ObjectMapper rosettaObjectMapper, URL resourcesPath, RegReportPaths paths) {
        super(classLoader, rosettaObjectMapper, resourcesPath, paths, null, ReportIdentifierDataSet.class, true);
    }

    @Override
    public List<ReportIdentifierDataSet> load() {
        throw new UnsupportedOperationException("this is intended for use with a pre-existing file");
    }

    @Override
    protected ReportIdentifierDataSet loadInputFiles(ReportIdentifierDataSet descriptor) {
        List<ReportDataItem> loadedData = new ArrayList<>();
        ReportDataSet dataSet = descriptor.getDataSet();
        for (ReportDataItem data : dataSet.getData()) {
            ReportDataItem reportDataItem = new ReportDataItem(data.getName(),
                    data.getInput(),
                    getExpected(descriptor.getReportIdentifier(), dataSet.getDataSetName(), dataSet.getExpectedType(), data));
            loadedData.add(reportDataItem);
        }
        return new ReportIdentifierDataSet(
                descriptor.getReportIdentifier(),
                new ReportDataSet(dataSet.getDataSetName(), dataSet.getInputType(), dataSet.getApplicableReports(), loadedData));
    }

    private Object getExpected(RegReportIdentifier regReportIdentifier, String dataSetName, String expectedType, ReportDataItem data) {
        if (data.getInput() instanceof String) {
            // attempt to load per report expectation file
            Path inputFileName = Paths.get(String.valueOf(data.getInput()));
            Path keyValueExpectationRelativePath = paths.getKeyValueExpectationFilePath(regReportIdentifier, dataSetName, inputFileName);
            Path keyValueExpectationPath = UrlUtils.toPath(resourcesPath).resolve(keyValueExpectationRelativePath);
            if (Files.exists(keyValueExpectationPath)) {
                URL keyValueExpectationUrl = UrlUtils.toUrl(keyValueExpectationPath);
                List<ExpectedResultField> resultFields = readTypeList(ExpectedResultField.class, rosettaObjectMapper, keyValueExpectationUrl);
                ExpectedResult expectedResult =
                        data.getExpected() == null ?
                                new ExpectedResult(new HashMap<>()) :
                                fromObject(data.getExpected(), ExpectedResult.class, rosettaObjectMapper);
                expectedResult.getExpectationsPerReport().put(regReportIdentifier.getName(), resultFields);
                return expectedResult;
            }
        }
        // for backwards compatibility
        if (data.getExpected() == null) {
            return null;
        }
        else {
            // attempt to get the legacy all-reports expectations file
            Class<?> expectedTypeClass = loadClass(expectedType, classLoader);
            if (data.getExpected() instanceof String) {
                // by path
                String expectedFileName = (String) data.getExpected();
                Path expectedFilePath = paths.getOutputPath().resolve(expectedFileName);
                return readType(expectedTypeClass, rosettaObjectMapper, resolve(expectedFilePath.toString()));
            } else {
                // by object
                return fromObject(data.getExpected(), expectedTypeClass, rosettaObjectMapper);
            }
        }
    }
}