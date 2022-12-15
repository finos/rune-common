package com.regnosys.rosetta.common.serialisation.reportdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.regnosys.rosetta.common.reports.RegReportIdentifier;
import com.regnosys.rosetta.common.reports.RegReportPaths;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class JsonExpectedResultLoaderTest {

    private static final Path RESOURCES_PATH = Paths.get("src/test/resources");

    private ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getNewRosettaObjectMapper();

    @Test
    void shouldLoadExpectedResultForReport1() throws IOException {
        RegReportPaths paths = RegReportPaths.getDefault(Paths.get("regs/test-use-case-load-expected"));

        JsonExpectedResultLoader jsonExpectedResultLoader = getJsonExpectedResultLoader(paths);

        String reportName = "report1";
        ReportIdentifierDataSet inputDataSet = getReportIdentifierDataSet(paths, reportName);

        ReportIdentifierDataSet enrichedDataSet = jsonExpectedResultLoader.loadInputFiles(inputDataSet);

        assertNotNull(enrichedDataSet.getDataSet());
        List<ReportDataItem> data = enrichedDataSet.getDataSet().getData();
        assertEquals(2, data.size());

        ReportDataItem reportDataItem1 = data.get(0);
        assertEquals("Name 1", reportDataItem1.getName());
        assertEquals("test1.json", reportDataItem1.getInput());

        ExpectedResult expectedResult = (ExpectedResult) reportDataItem1.getExpected();
        List<ExpectedResultField> keyValueResults = expectedResult.getExpectationsPerReport().get(reportName);
        assertNotNull(keyValueResults);
        assertEquals(1, keyValueResults.size());

        ExpectedResultField expectedResultField = keyValueResults.get(0);
        assertEquals("column 1", expectedResultField.getName());
        assertEquals("NewTrade-expected", expectedResultField.getValue());

        ReportDataItem reportDataItem2 = data.get(1);
        assertEquals("Name 2", reportDataItem2.getName());
        assertNotNull(reportDataItem2.getInput());
        assertNull(reportDataItem2.getExpected());
    }

    @Test
    void shouldLoadExpectedResultFromLegacyFolderStructure() throws IOException {
        RegReportPaths paths = RegReportPaths.getLegacy(Paths.get("regs/test-use-case-load-expected-legacy"));

        JsonExpectedResultLoader jsonExpectedResultLoader = getJsonExpectedResultLoader(paths);

        String reportName = "report1";
        ReportIdentifierDataSet inputDataSet = getReportIdentifierDataSet(paths, reportName);

        ReportIdentifierDataSet enrichedDataSet = jsonExpectedResultLoader.loadInputFiles(inputDataSet);

        // data set
        assertNotNull(enrichedDataSet.getDataSet());
        List<ReportDataItem> data = enrichedDataSet.getDataSet().getData();
        assertEquals(2, data.size());

        // data item 1
        ReportDataItem reportDataItem1 = data.get(0);
        assertEquals("Name 1", reportDataItem1.getName());
        assertEquals("test1.json", reportDataItem1.getInput());

        // data item 1 - expected results for report 1
        ExpectedResult expectedResult = (ExpectedResult) reportDataItem1.getExpected();
        Map<String, List<ExpectedResultField>> expectationsMap1 = expectedResult.getExpectationsPerReport();

        List<ExpectedResultField> keyValueResultsReport1 = expectationsMap1.get("report1");
        assertNotNull(keyValueResultsReport1);
        assertEquals(1, keyValueResultsReport1.size());

        ExpectedResultField expectedResultField = keyValueResultsReport1.get(0);
        assertEquals("column 1", expectedResultField.getName());
        assertEquals("NewTrade-expected", expectedResultField.getValue());

        List<ExpectedResultField> keyValueResults1Report2 = expectationsMap1.get("report2");
        assertNull(keyValueResults1Report2);

        // data item 2
        ReportDataItem reportDataItem2 = data.get(1);
        assertEquals("Name 2", reportDataItem2.getName());
        assertNotNull(reportDataItem2.getInput());

        // data item 2 - expected results for report 1
        ExpectedResult expectedResult2 = (ExpectedResult) reportDataItem2.getExpected();
        Map<String, List<ExpectedResultField>> expectationsMap2 = expectedResult2.getExpectationsPerReport();

        List<ExpectedResultField> keyValueResults2Report1 = expectationsMap2.get("report1");
        assertNull(keyValueResults2Report1);

        List<ExpectedResultField> keyValueResults2Report2 = expectationsMap2.get("report2");
        assertNotNull(keyValueResults2Report2);
        assertEquals(1, keyValueResults2Report2.size());

        ExpectedResultField expectedResultField2 = keyValueResults2Report2.get(0);
        assertEquals("column 2", expectedResultField2.getName());
        assertEquals("TerminatedTrade-expected", expectedResultField2.getValue());
    }

    private JsonExpectedResultLoader getJsonExpectedResultLoader(RegReportPaths paths) throws MalformedURLException {
        JsonExpectedResultLoader jsonExpectedResultLoader =
                new JsonExpectedResultLoader(this.getClass().getClassLoader(),
                        rosettaObjectMapper,
                        RESOURCES_PATH.toUri().toURL(),
                        paths);
        return jsonExpectedResultLoader;
    }

    private ReportIdentifierDataSet getReportIdentifierDataSet(RegReportPaths paths, String reportName) throws IOException {
        RegReportIdentifier reportIdentifier = new RegReportIdentifier(null, null, reportName, null);

        URL url = Resources.getResource(paths.getDescriptorPath("regulatory-reporting-data-descriptor.json").toString());
        String json = Resources.toString(url, StandardCharsets.UTF_8);
        List<ReportDataSet> reportDataSets = rosettaObjectMapper.readValue(json, rosettaObjectMapper.getTypeFactory().constructCollectionType(List.class, ReportDataSet.class));
        ReportDataSet reportDataSet = reportDataSets.get(0);

        return new ReportIdentifierDataSet(reportIdentifier, reportDataSet);
    }
}
