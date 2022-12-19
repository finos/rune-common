package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.lookup.JsonLookupDataLoader;
import com.regnosys.rosetta.common.serialisation.lookup.LookupDataSet;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonLookupDataLoaderTest {

    private static final Path RESOURCES_PATH = Paths.get("src/test/resources");

    private final ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getNewRosettaObjectMapper();

    @Test
    void lookupsLoaded() throws MalformedURLException {
        // descriptor and input on same path
        Path path = RESOURCES_PATH.resolve("regs/test-reg-lookups");

        List<LookupDataSet> lookupDataSets = loadLookupDataSets(path, path);

        assertEquals(lookupDataSets.size(), 2);
        assertEquals(lookupDataSets.get(0).getName(), "IsExecutingEntityInvestmentFirm");
        assertEquals(lookupDataSets.get(0).getData().size(), 1);
        assertEquals(lookupDataSets.get(1).getName(), "ExecutingEntity");
        assertEquals(lookupDataSets.get(1).getData().size(), 4);
    }

    @Test
    void descriptorPathDoesNotExist() throws MalformedURLException {
        // descriptor and input on same path
        Path path = RESOURCES_PATH.resolve("not-found");

        List<LookupDataSet> lookupDataSets = loadLookupDataSets(path, path);

        assertEquals(lookupDataSets.size(), 0);
    }

    @Test
    void descriptorPathDoesNotDoesNotContainDescriptorFile() throws MalformedURLException {
        // descriptor and input on same path
        Path path = RESOURCES_PATH.resolve("test-workspaces");

        List<LookupDataSet> lookupDataSets = loadLookupDataSets(path, path);

        assertEquals(lookupDataSets.size(), 0);
    }

    private List<LookupDataSet> loadLookupDataSets(Path lookupDescriptorPath, Path inputPath) throws MalformedURLException {
        return new JsonLookupDataLoader(this.getClass().getClassLoader(),
                rosettaObjectMapper,
                lookupDescriptorPath.toUri().toURL(),
                Collections.singletonList(JsonLookupDataLoader.DEFAULT_DESCRIPTOR_NAME),
                inputPath.toUri().toURL()).load();
    }

    @SuppressWarnings("unused") // loaded at runtime
    static class ExecutingEntity {
        public Address addressOfBranch;
        public Address addressOfIncorporation;
        public Boolean isInvestmentFirm;
    }

    static class Address {
        public Country country;
    }

    static class Country {
        public String value;
    }
}
