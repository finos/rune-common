package com.regnosys.rosetta.common.serialisation

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

data class EventTestModelObject(val eventDate: LocalDate, val eventQualifier: String)

internal class JsonReportDataLoaderTest {

    private val rosettaObjectMapper = RosettaObjectMapper.getNewRosettaObjectMapper()

    @Test
    internal fun useCasesLoaded() {
        val reportDataSets = JsonReportDataLoader(this.javaClass.classLoader, rosettaObjectMapper, "regs/test-use-case-load").load()

        assertSame(reportDataSets.size, 1)
        assertSame(reportDataSets[0].data.size, 2)

        assertTrue(reportDataSets[0].data[0].input is EventTestModelObject)
        assertTrue(reportDataSets[0].data[1].input is EventTestModelObject)

        assertEquals(reportDataSets[0].data[0], ReportDataItem(
                name = "This is the desc of the usecase",
                input = EventTestModelObject(LocalDate.parse("2018-02-20"), "NewTrade")))
        assertEquals(reportDataSets[0].data[1], ReportDataItem(
                name = "This is the desc of the another usecase that has inline json rather then a file",
                input = EventTestModelObject(LocalDate.parse("2018-02-21"), "TerminatedTrade")))
    }

    @Test
    internal fun descriptorPathDoesNotExist() {
        val reportDataSets = JsonReportDataLoader(this.javaClass.classLoader, rosettaObjectMapper, "not-found").load()
        assertSame(reportDataSets.size, 0)
    }

    @Test
    internal fun descriptorPathDoesNotDoesNotContainDescriptorFile() {
        val reportDataSets = JsonReportDataLoader(this.javaClass.classLoader, rosettaObjectMapper, "test-workspaces").load()
        assertSame(reportDataSets.size, 0)
    }
}
