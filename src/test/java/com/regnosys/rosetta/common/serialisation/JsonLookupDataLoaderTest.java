package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.lookup.JsonLookupDataLoader;
import com.regnosys.rosetta.common.serialisation.lookup.LookupDataSet;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonLookupDataLoaderTest {
    private static final List<String> DESCRIPTOR_FILE_NAMES = Collections.singletonList(JsonLookupDataLoader.DEFAULT_DESCRIPTOR_NAME);

    private ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getNewRosettaObjectMapper();

    @Test
    void lookupsLoaded() {
        List<LookupDataSet> lookupDataSets = new JsonLookupDataLoader(this.getClass().getClassLoader(), rosettaObjectMapper, Paths.get("src/test/resources/regs/test-reg-lookups").toUri(), DESCRIPTOR_FILE_NAMES).load();
        assertEquals(lookupDataSets.size(), 2);
        assertEquals(lookupDataSets.get(0).getName(), "IsExecutingEntityInvestmentFirm");
        assertEquals(lookupDataSets.get(0).getData().size(), 1);
        assertEquals(lookupDataSets.get(1).getName(), "ExecutingEntity");
        assertEquals(lookupDataSets.get(1).getData().size(), 4);
    }

    @Test
    void descriptorPathDoesNotExist() {
        List<LookupDataSet> lookupDataSets = new JsonLookupDataLoader(this.getClass().getClassLoader(), rosettaObjectMapper, Paths.get("not-found").toUri(), DESCRIPTOR_FILE_NAMES).load();
        assertEquals(lookupDataSets.size(), 0);
    }

    @Test
    void descriptorPathDoesNotDoesNotContainDescriptorFile() {
        List<LookupDataSet> lookupDataSets = new JsonLookupDataLoader(this.getClass().getClassLoader(), rosettaObjectMapper, Paths.get("src/test/resources/test-workspaces").toUri(), DESCRIPTOR_FILE_NAMES).load();
        assertEquals(lookupDataSets.size(), 0);
    }

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
