package com.regnosys.rosetta.common.serialisation.json.projection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import com.regnosys.rosetta.common.serialisation.projectiondata.JsonProjectionDataLoader;
import com.regnosys.rosetta.common.serialisation.projectiondata.ProjectionDataSet;
import com.regnosys.rosetta.common.serialisation.reportdata.ReportDataItem;
import com.rosetta.model.lib.ModelReportId;
import com.rosetta.util.DottedPath;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class JsonProjectionDataLoaderTest {

    private static final Path RESOURCES_PATH = Paths.get("src/test/resources");

    private static final List<String> DESCRIPTOR_FILE_NAMES = Collections.singletonList("projection-" + JsonProjectionDataLoader.DEFAULT_DESCRIPTOR_NAME);

    private final ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getNewRosettaObjectMapper();


    @Test
    void descriptorPathDoesNotExist() throws MalformedURLException {
        // descriptor and input on same path
        Path path = RESOURCES_PATH.resolve("not-found");

        List<ProjectionDataSet> projectionDataSets = loadProjectionDataSets(path, path);

        assertEquals(projectionDataSets.size(), 0);
    }

    @Test
    void loadDescriptorPathCreateProjectionDataSet() throws MalformedURLException {
        // descriptor and input on same path
        Path path = RESOURCES_PATH.resolve("projection/test-use-case-load");

        List<ProjectionDataSet> projectionDataSets = loadProjectionDataSets(path, path);

        assertEquals(1, projectionDataSets.size());
        ProjectionDataSet projectionDataSet1 = projectionDataSets.get(0);
        assertEquals("Test set 1", projectionDataSet1.getDataSetName());
        assertEquals("Testset1", projectionDataSet1.getDataSetShortName());
        assertEquals(Lists.newArrayList("com.regnosys.rosetta.common.serialisation.json.projection.JsonProjectionDataLoaderTest", "com.regnosys.rosetta.common.serialisation.json.projection.JsonProjectionDataLoaderTest2"), projectionDataSet1.getApplicableProjections());
        ModelReportId expectedApplicableReport = new ModelReportId(DottedPath.splitOnDots("com.regnosys.rosetta.common.serialisation.json.projection"), "REPORT_BODY", "REPORT_CORPUS");
        assertEquals(Lists.newArrayList(expectedApplicableReport), projectionDataSet1.getApplicableReports());

        assertEquals(2, projectionDataSet1.getData().size());

        assertTrue(projectionDataSet1.getData().get(0).getInput() instanceof EventTestModelObject);
        assertEquals(new ReportDataItem("This is the desc of the usecase", new EventTestModelObject(LocalDate.parse("2018-02-20"), "NewTrade"), null), projectionDataSet1.getData().get(0));
    }

    @Test
    void createProjectionDataSetFromNullApplicableProjections() throws MalformedURLException {
        // descriptor and input on same path
        Path path = RESOURCES_PATH.resolve("projection/null-applicable-projections");

        List<ProjectionDataSet> projectionDataSets = loadProjectionDataSets(path, path);

        assertEquals(1, projectionDataSets.size());
        ProjectionDataSet projectionDataSet1 = projectionDataSets.get(0);
        assertEquals("Test set 2", projectionDataSet1.getDataSetName());
        assertEquals("Testset2", projectionDataSet1.getDataSetShortName());

        List<String> applicableProjections = projectionDataSet1.getApplicableProjections();
        assertEquals(0, applicableProjections.size());

    }
    private List<ProjectionDataSet> loadProjectionDataSets(Path descriptorPath, Path inputPath) throws MalformedURLException {
        return new JsonProjectionDataLoader(this.getClass().getClassLoader(),
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
