package com.regnosys.rosetta.common.serialisation.reportdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.google.common.io.Resources;
import com.regnosys.rosetta.common.serialisation.DataItem;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import com.rosetta.model.lib.ModelReportId;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static com.regnosys.rosetta.common.reports.RegReportPaths.CONFIG_PATH;
import static com.regnosys.rosetta.common.reports.RegReportPaths.OUTPUT_PATH;
import static org.junit.jupiter.api.Assertions.*;

class JsonExpectedResultLoaderTest {

    private static final Path RESOURCES_PATH = Paths.get("src/test/resources");

    private final ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getNewRosettaObjectMapper();

    @Test
    void shouldLoadExpectedResultForReport1() throws IOException {
        Path rootPath = Paths.get("regs/test-use-case-load-expected");

        ModelReportId reportId = ModelReportId.fromNamespaceAndRegulatoryReferenceString("test.reg.<Body Corpus1>");
        Path descriptorPath = rootPath.resolve(CONFIG_PATH);
        ReportIdentifierDataSet inputDataSet = getReportIdentifierDataSet(descriptorPath, reportId);

        // test
        URL outputUrl = RESOURCES_PATH.resolve(rootPath).resolve(OUTPUT_PATH).toUri().toURL();
        JsonExpectedResultLoader jsonExpectedResultLoader = getJsonExpectedResultLoader(outputUrl);
        ReportIdentifierDataSet enrichedDataSet = jsonExpectedResultLoader.loadInputFiles(inputDataSet);

        assertNotNull(enrichedDataSet.getDataSet());
        List<DataItem> data = enrichedDataSet.getDataSet().getData();
        assertEquals(2, data.size());

        DataItem dataItem1 = data.get(0);
        assertEquals("Name 1", dataItem1.getName());
        assertEquals("test1.json", dataItem1.getInput());

        ExpectedResult expectedResult = (ExpectedResult) dataItem1.getExpected();
        List<ExpectedResultField> keyValueResults = expectedResult.getExpectationsPerReport().get(reportId);
        assertNotNull(keyValueResults);
        assertEquals(1, keyValueResults.size());

        ExpectedResultField expectedResultField = keyValueResults.get(0);
        assertEquals("column 1", expectedResultField.getName());
        assertEquals("NewTrade-expected", expectedResultField.getValue());

        DataItem dataItem2 = data.get(1);
        assertEquals("Name 2", dataItem2.getName());
        assertNotNull(dataItem2.getInput());
        assertNull(dataItem2.getExpected());
    }

    @Test
    void shouldLoadExpectedResultFromLegacyFolderStructure() throws IOException {
        Path rootPath = Paths.get("regs/test-use-case-load-expected-legacy");

        ModelReportId reportId1 = ModelReportId.fromNamespaceAndRegulatoryReferenceString("test.reg.<Body Corpus1>");
        ReportIdentifierDataSet inputDataSet = getReportIdentifierDataSet(rootPath, reportId1);

        ModelReportId reportId2 = ModelReportId.fromNamespaceAndRegulatoryReferenceString("test.reg.<Body Corpus2>");

        // test
        URL outputUrl = RESOURCES_PATH.resolve(rootPath).toUri().toURL();
        JsonExpectedResultLoader jsonExpectedResultLoader = getJsonExpectedResultLoader(outputUrl);
        ReportIdentifierDataSet enrichedDataSet = jsonExpectedResultLoader.loadInputFiles(inputDataSet);

        // data set
        assertNotNull(enrichedDataSet.getDataSet());
        List<DataItem> data = enrichedDataSet.getDataSet().getData();
        assertEquals(2, data.size());

        // data item 1
        DataItem dataItem1 = data.get(0);
        assertEquals("Name 1", dataItem1.getName());
        assertEquals("test1.json", dataItem1.getInput());

        // data item 1 - expected results for report 1
        ExpectedResult expectedResult = (ExpectedResult) dataItem1.getExpected();
        Map<ModelReportId, List<ExpectedResultField>> expectationsMap1 = expectedResult.getExpectationsPerReport();

        List<ExpectedResultField> keyValueResultsReport1 = expectationsMap1.get(reportId1);
        assertNotNull(keyValueResultsReport1);
        assertEquals(1, keyValueResultsReport1.size());

        ExpectedResultField expectedResultField = keyValueResultsReport1.get(0);
        assertEquals("column 1", expectedResultField.getName());
        assertEquals("NewTrade-expected", expectedResultField.getValue());

        List<ExpectedResultField> keyValueResults1Report2 = expectationsMap1.get(reportId2);
        assertNull(keyValueResults1Report2);

        // data item 2
        DataItem dataItem2 = data.get(1);
        assertEquals("Name 2", dataItem2.getName());
        assertNotNull(dataItem2.getInput());

        // data item 2 - expected results for report 1
        ExpectedResult expectedResult2 = (ExpectedResult) dataItem2.getExpected();
        Map<ModelReportId, List<ExpectedResultField>> expectationsMap2 = expectedResult2.getExpectationsPerReport();

        List<ExpectedResultField> keyValueResults2Report1 = expectationsMap2.get(reportId1);
        assertNull(keyValueResults2Report1);

        List<ExpectedResultField> keyValueResults2Report2 = expectationsMap2.get(reportId2);
        assertNotNull(keyValueResults2Report2);
        assertEquals(1, keyValueResults2Report2.size());

        ExpectedResultField expectedResultField2 = keyValueResults2Report2.get(0);
        assertEquals("column 2", expectedResultField2.getName());
        assertEquals("TerminatedTrade-expected", expectedResultField2.getValue());
    }

    private JsonExpectedResultLoader getJsonExpectedResultLoader(URL outputPath) {
        return new JsonExpectedResultLoader(this.getClass().getClassLoader(),
                rosettaObjectMapper,
                outputPath);
    }

    private ReportIdentifierDataSet getReportIdentifierDataSet(Path descriptorPath, ModelReportId reportId) throws IOException {
        URL url = Resources.getResource(descriptorPath.resolve("regulatory-reporting-data-descriptor.json").toString());
        String json = Resources.toString(url, StandardCharsets.UTF_8);
        CollectionType loadType = rosettaObjectMapper.getTypeFactory().constructCollectionType(List.class, ReportDataSet.class);
        List<ReportDataSet> reportDataSets = rosettaObjectMapper.readValue(json, loadType);
        ReportDataSet reportDataSet = reportDataSets.get(0);

        return new ReportIdentifierDataSet(reportId, reportDataSet);
    }
}
