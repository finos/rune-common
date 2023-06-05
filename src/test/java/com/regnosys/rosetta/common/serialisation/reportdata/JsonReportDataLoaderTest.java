package com.regnosys.rosetta.common.serialisation.reportdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;

class JsonReportDataLoaderTest {

    private static final Path RESOURCES_PATH = Paths.get("src/test/resources");

    private static final List<String> DESCRIPTOR_FILE_NAMES = Collections.singletonList(JsonReportDataLoader.DEFAULT_DESCRIPTOR_NAME);

    private final ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getNewRosettaObjectMapper();

    @Test
    void lookupsLoaded() throws MalformedURLException {
        // descriptor and input on same path
        Path path = RESOURCES_PATH.resolve("regs/test-use-case-load");

        List<ReportDataSet> reportDataSets = loadReportDataSets(path, path);

        assertEquals(reportDataSets.size(), 1);
        assertEquals(reportDataSets.get(0).getData().size(), 2);

        assertTrue(reportDataSets.get(0).getData().get(0).getInput() instanceof EventTestModelObject);
        assertTrue(reportDataSets.get(0).getData().get(1).getInput() instanceof EventTestModelObject);

        assertEquals(new ReportDataItem("This is the desc of the usecase",
                        new EventTestModelObject(LocalDate.parse("2018-02-20"), "NewTrade"),
                        null),
                reportDataSets.get(0).getData().get(0));
        assertEquals(new ReportDataItem("This is the desc of the another usecase that has inline json rather then a file",
                        new EventTestModelObject(LocalDate.parse("2018-02-21"), "TerminatedTrade"),
                        null),
                reportDataSets.get(0).getData().get(1));
    }

    @Test
    void descriptorPathDoesNotExist() throws MalformedURLException {
        // descriptor and input on same path
        Path path = RESOURCES_PATH.resolve("not-found");

        List<ReportDataSet> reportDataSets = loadReportDataSets(path, path);

        assertEquals(reportDataSets.size(), 0);
    }

    @Test
    void descriptorPathDoesNotDoesNotContainDescriptorFile() throws MalformedURLException {
        // descriptor and input on same path
        Path path = RESOURCES_PATH.resolve("test-workspaces");

        List<ReportDataSet> reportDataSets = loadReportDataSets(path, path);

        assertEquals(reportDataSets.size(), 0);
    }

    @Test
    void lookupsLoadedWithAllErrors() throws MalformedURLException {
        // descriptor and input on same path
        Path path = RESOURCES_PATH.resolve("regs/test-use-case-load-error");

        List<ReportDataSet> reportDataSets = loadReportDataSets(path, path);

        assertEquals(reportDataSets.size(), 1);
        assertEquals(reportDataSets.get(0).getData().size(), 2);

        assertNotNull(reportDataSets.get(0).getData().get(0).getError());
        assertNotNull(reportDataSets.get(0).getData().get(1).getError());

        assertThrows(RuntimeException.class, () -> reportDataSets.get(0).getData().get(0).getInput());
        assertThrows(RuntimeException.class, () -> reportDataSets.get(0).getData().get(1).getInput());
    }
    @Test
    void lookupsLoadedWithOneError() throws MalformedURLException {
        // descriptor and input on same path
        Path path = RESOURCES_PATH.resolve("regs/test-use-case-load-one-error");

        List<ReportDataSet> reportDataSets = loadReportDataSets(path, path);

        assertEquals(reportDataSets.size(), 1);
        assertEquals(reportDataSets.get(0).getData().size(), 2);

        assertNull(reportDataSets.get(0).getData().get(0).getError());
        assertNotNull(reportDataSets.get(0).getData().get(1).getError());

        assertThrows(RuntimeException.class, () -> reportDataSets.get(0).getData().get(1).getInput());

        assertTrue(reportDataSets.get(0).getData().get(0).getInput() instanceof EventTestModelObject);
        assertEquals(new ReportDataItem("This is the desc of the usecase",
                        new EventTestModelObject(LocalDate.parse("2018-02-20"), "NewTrade"),
                        null),
                reportDataSets.get(0).getData().get(0));

    }

    private List<ReportDataSet> loadReportDataSets(Path descriptorPath, Path inputPath) throws MalformedURLException {
        return new JsonReportDataLoader(this.getClass().getClassLoader(),
                rosettaObjectMapper,
                descriptorPath.toUri().toURL(),
                DESCRIPTOR_FILE_NAMES,
                inputPath.toUri().toURL()).load();
    }

    static class EventTestModelObject {
        public LocalDate eventDate;
        public String eventQualifier;

        public EventTestModelObject() {

        }

        public EventTestModelObject(LocalDate eventDate, String eventQualifier) {
            this.eventDate = eventDate;
            this.eventQualifier = eventQualifier;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EventTestModelObject that = (EventTestModelObject) o;
            return Objects.equals(eventDate, that.eventDate) &&
                    Objects.equals(eventQualifier, that.eventQualifier);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventDate, eventQualifier);
        }
    }
}
