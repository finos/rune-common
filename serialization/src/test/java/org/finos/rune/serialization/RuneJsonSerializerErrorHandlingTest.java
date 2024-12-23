package org.finos.rune.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import com.regnosys.rosetta.RosettaStandaloneSetup;
import com.regnosys.rosetta.tests.util.CodeGeneratorTestHelper;
import com.rosetta.model.lib.RosettaModelObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.finos.rune.serialization.RuneSerializerTestHelper.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Disabled
public class RuneJsonSerializerErrorHandlingTest {
    public static final String TEST_TYPE = "rune-serializer-error-handling-test";
    private RuneJsonSerializer runeJsonSerializer;

    private static CodeGeneratorTestHelper helper;

    @BeforeAll
    static void beforeAll() {
        RosettaStandaloneSetup rosettaStandaloneSetup = new RosettaStandaloneSetup();
        Injector injector = rosettaStandaloneSetup.createInjectorAndDoEMFRegistration();
        helper = injector.getInstance(CodeGeneratorTestHelper.class);
    }

    @BeforeEach
    void setUp() {
        runeJsonSerializer = new RuneJacksonJsonSerializer();
    }


    @Test
    public void testCardinalityTooManyElementsThrowsException() {
        Path groupPath = getGroupPath("cardinality");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath, "cardinality.rosetta");
        String json = readAsString(getFile(groupPath, "too-many-elements.json"));

        RosettaModelObject deserializedObject = runeJsonSerializer.fromJson(json, rootDataType);

        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> {
            runeJsonSerializer.toJson(deserializedObject);
        });

        assertEquals("Attribute contained more than the allowed number of elements", runtimeException.getMessage());
    }

    @Test
    public void testNumberFractionTooLargeThrowsException() {
        Path groupPath = getGroupPath("parameterised");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath, "parameterised.rosetta");
        String json = readAsString(getFile(groupPath, "number-fraction-too-large.json"));

        RosettaModelObject deserializedObject = runeJsonSerializer.fromJson(json, rootDataType);

        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> {
            runeJsonSerializer.toJson(deserializedObject);
        });

        assertEquals("Number contained more than the allowed number of digits", runtimeException.getMessage());
    }

    @Test
    public void testNumberTooLargeThrowsException() {
        Path groupPath = getGroupPath("parameterised");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath, "parameterised.rosetta");
        String json = readAsString(getFile(groupPath, "number-too-large.json"));

        RosettaModelObject deserializedObject = runeJsonSerializer.fromJson(json, rootDataType);

        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> {
            runeJsonSerializer.toJson(deserializedObject);
        });

        assertEquals("Number contained more than the allowed number of digits", runtimeException.getMessage());
    }

    @Test
    public void testStringIllegalPatternThrowsException() {
        Path groupPath = getGroupPath("parameterised");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath, "parameterised.rosetta");
        String json = readAsString(getFile(groupPath, "string-illegal-pattern.json"));

        RosettaModelObject deserializedObject = runeJsonSerializer.fromJson(json, rootDataType);

        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> {
            runeJsonSerializer.toJson(deserializedObject);
        });

        assertEquals("String does not match the required pattern", runtimeException.getMessage());
    }

    @Test
    public void testStringTooLargeThrowsException() {
        Path groupPath = getGroupPath("parameterised");
        Class<RosettaModelObject> rootDataType = getRootRosettaModelObjectClass(groupPath, "parameterised.rosetta");
        String json = readAsString(getFile(groupPath, "string-too-large.json"));

        RosettaModelObject deserializedObject = runeJsonSerializer.fromJson(json, rootDataType);

        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> {
            runeJsonSerializer.toJson(deserializedObject);
        });

        assertEquals("String exceeds the maximum length", runtimeException.getMessage());
    }

    private static Path getGroupPath(String groupName) {
        return  Paths.get("src/test/resources").resolve(TEST_TYPE).resolve(groupName);
    }

    private Class<RosettaModelObject> getRootRosettaModelObjectClass(Path groupPath, String fileName) {
        Path rosetta = getFile(groupPath, fileName);
        String groupName = groupPath.getFileName().toString();
        return generateCompileAndGetRootDataType(groupName, Collections.singletonList(rosetta), helper, new DynamicCompiledClassLoader());
    }
}
