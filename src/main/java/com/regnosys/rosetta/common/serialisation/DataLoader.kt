package com.regnosys.rosetta.common.serialisation

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.regnosys.rosetta.common.util.ClassPathUtils
import java.util.*

/**
 * Interface to lookup model related data from an external source. The data is typically a model instance that can be
 * loaded from a json source (e.g. file, rest api).
 */
interface DataLoader<T> {
    fun load(): List<T>

    val rosettaObjectMapper: ObjectMapper
    val classLoader: ClassLoader
    val descriptorPath: String

    fun fromObject(obj: Any, type: Class<*>?) =
        rosettaObjectMapper.readValue(rosettaObjectMapper.writeValueAsString(obj), type)!!

    fun fromClasspath(file: String, type: Class<*>?) =
        ClassPathUtils.loadFromClasspath("$descriptorPath/${file}", classLoader).findFirst()
            .map { inputPath -> rosettaObjectMapper.readValue(inputPath.toUri().toURL(), type) }
            .orElseThrow { RuntimeException("Could not load $file of type $type") }!!
}

/**
 * Reg reporting allows data lookups from external ref data sources. The lookup data is essentially a map where the kay and value are
 * both rosetta model objects.
 */
data class LookupDataSet(val name: String, val keyType: String, val valueType: String, val data: List<LookupDataItem>)
data class LookupDataItem(val key: Any, val value: Any)

/**
 * Looks up the external ref data from a static file. The LookupDataItem.key/LookupDataItem.value can either be the Rosetta model object of type
 * LookupDataSet.keyType/LookupDataSet.valueType or a JSON file available form the classpath.
 */
class JsonLookupDataLoader(override val classLoader: ClassLoader,
                           override val rosettaObjectMapper: ObjectMapper,
                           override val descriptorPath: String,
                           private val descriptorFileName: String = "regulatory-reporting-lookup-descriptor.json") : DataLoader<LookupDataSet> {

    override fun load(): List<LookupDataSet> {
        return ClassPathUtils.findPathsFromClassPath(listOf(descriptorPath), ".*$descriptorFileName", Optional.empty(), classLoader)
            .map { rosettaObjectMapper.readValue<List<LookupDataSet>>(it.toUri().toURL(), object : TypeReference<List<LookupDataSet>>() {}) }.flatten()
            .map { loadInputFiles(it) }
    }

    private fun loadInputFiles(descriptor: LookupDataSet): LookupDataSet {
        val keyType = classLoader.loadClass(descriptor.keyType)
        val valueType = classLoader.loadClass(descriptor.valueType)

        val loadedData = descriptor.data.map { data ->
            val key =
                if (data.key == "*" || descriptor.keyType == String::class.java.name) data.key
                else if (data.key is String) fromClasspath(data.key, keyType)
                else fromObject(data.key, keyType)

            val value = if (data.value is String) fromClasspath(data.value, valueType) else fromObject(data.value, valueType)
            LookupDataItem(key, value)
        }
        return LookupDataSet(descriptor.name, descriptor.keyType, descriptor.valueType, loadedData)
    }
}

/**
 * Reg reporting use cases. The use case is defined by the a dataSetName, what reports it applies to (or * for all reports),
 * and a list of model instances: ReportDataItem.input. Each input must have a name that uniquly identifies it (e.f. a number or the name of the usecase)
 * both rosetta model objects.
 */
data class ReportDataSet(val dataSetName: String, val inputType: String, val applicableReports: String, val data: List<ReportDataItem>)
data class ReportDataItem(val name: String, val input: Any)

/**
 * Reads the use-cases from a static file. The ReportDataItem.input can either be the Rosetta model object of type
 * ReportDataSet.inputType or a JSON file available form the classpath.
 */
class JsonReportDataLoader(override val classLoader: ClassLoader,
                           override val rosettaObjectMapper: ObjectMapper,
                           override val descriptorPath: String,
                           private val descriptorFileName: String = "regulatory-reporting-data-descriptor.json") : DataLoader<ReportDataSet> {

    override fun load(): List<ReportDataSet> {
        return ClassPathUtils.findPathsFromClassPath(listOf(descriptorPath), ".*$descriptorFileName", Optional.empty(), classLoader)
            .map { rosettaObjectMapper.readValue<List<ReportDataSet>>(it.toUri().toURL(), object : TypeReference<List<ReportDataSet>>() {}) }.flatten()
            .map { loadInputFiles(it) }
    }

    private fun loadInputFiles(descriptor: ReportDataSet): ReportDataSet {
        val inputType = classLoader.loadClass(descriptor.inputType)
        val loadedData = descriptor.data.map { data ->
            ReportDataItem(data.name, if (data.input is String) fromClasspath(data.input, inputType) else fromObject(data.input, inputType))
        }
        return ReportDataSet(descriptor.dataSetName, descriptor.inputType, descriptor.applicableReports, loadedData)
    }
}