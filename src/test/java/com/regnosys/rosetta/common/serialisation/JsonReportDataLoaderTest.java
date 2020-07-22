package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.reportdata.JsonReportDataLoader;
import com.regnosys.rosetta.common.serialisation.reportdata.ReportDataItem;
import com.regnosys.rosetta.common.serialisation.reportdata.ReportDataSet;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonReportDataLoaderTest {
    private ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getNewRosettaObjectMapper();

    @Test
    void lookupsLoaded() {
        List<ReportDataSet> reportDataSets = new JsonReportDataLoader(this.getClass().getClassLoader(), rosettaObjectMapper, Paths.get("src/test/resources/regs/test-use-case-load").toUri()).load();
        assertEquals(reportDataSets.size(), 1);
        assertEquals(reportDataSets.get(0).getData().size(), 2);

        assertTrue(reportDataSets.get(0).getData().get(0).getInput() instanceof EventTestModelObject);
        assertTrue(reportDataSets.get(0).getData().get(1).getInput() instanceof EventTestModelObject);

        assertEquals(reportDataSets.get(0).getData().get(0), new ReportDataItem(
                "This is the desc of the usecase",
                new EventTestModelObject(LocalDate.parse("2018-02-20"), "NewTrade")));
        assertEquals(reportDataSets.get(0).getData().get(1), new ReportDataItem(
                "This is the desc of the another usecase that has inline json rather then a file",
                new EventTestModelObject(LocalDate.parse("2018-02-21"), "TerminatedTrade")));
    }

    @Test
    void descriptorPathDoesNotExist() {
        List<ReportDataSet> reportDataSets = new JsonReportDataLoader(this.getClass().getClassLoader(), rosettaObjectMapper, Paths.get("not-found").toUri()).load();
        assertEquals(reportDataSets.size(), 0);
    }

    @Test
    void descriptorPathDoesNotDoesNotContainDescriptorFile() {
        List<ReportDataSet> reportDataSets = new JsonReportDataLoader(this.getClass().getClassLoader(), rosettaObjectMapper, Paths.get("src/test/resources/test-workspaces").toUri()).load();
        assertEquals(reportDataSets.size(), 0);
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
