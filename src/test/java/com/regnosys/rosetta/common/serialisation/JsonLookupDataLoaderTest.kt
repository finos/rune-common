package com.regnosys.rosetta.common.serialisation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

data class ExecutingEntity(val addressOfBranch: Address, val addressOfIncorporation: Address, val isInvestmentFirm: Boolean)
data class Address(val country: Country)
data class Country(val value: String)

internal class JsonLookupDataLoaderTest {
    private val rosettaObjectMapper = RosettaObjectMapper.getNewRosettaObjectMapper()

    @Test
    internal fun lookupsLoaded() {
        val lookupDataSets = JsonLookupDataLoader(this.javaClass.classLoader, rosettaObjectMapper, "regs/test-reg-lookups").load()
        assertEquals(lookupDataSets.size, 2)
        assertEquals(lookupDataSets[0].name, "IsExecutingEntityInvestmentFirm")
        assertEquals(lookupDataSets[0].data.size, 1)
        assertEquals(lookupDataSets[1].name, "ExecutingEntity")
        assertEquals(lookupDataSets[1].data.size, 4)
    }

    @Test
    internal fun descriptorPathDoesNotExist() {
        val lookupDataSets = JsonLookupDataLoader(this.javaClass.classLoader, rosettaObjectMapper, "not-found").load()
        assertSame(lookupDataSets.size, 0)
    }

    @Test
    internal fun descriptorPathDoesNotDoesNotContainDescriptorFile() {
        val lookupDataSets = JsonLookupDataLoader(this.javaClass.classLoader, rosettaObjectMapper, "test-workspaces").load()
        assertSame(lookupDataSets.size, 0)
    }
}