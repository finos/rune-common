package org.finos.rune.serialization;

import com.google.inject.Injector;
import com.regnosys.rosetta.RosettaStandaloneSetup;
import com.regnosys.rosetta.tests.util.CodeGeneratorTestHelper;
import com.rosetta.model.lib.RosettaModelObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.finos.rune.serialization.RuneSerializerTestHelper.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RuneSerializerErrorHandlingTest {
    public static final String TEST_TYPE = "rune-serializer-error-handling-test";
    private RuneSerializer runeSerializer;

    private static CodeGeneratorTestHelper helper;

    @BeforeAll
    static void beforeAll() {
        RosettaStandaloneSetup rosettaStandaloneSetup = new RosettaStandaloneSetup();
        Injector injector = rosettaStandaloneSetup.createInjectorAndDoEMFRegistration();
        helper = injector.getInstance(CodeGeneratorTestHelper.class);
    }

    @BeforeEach
    void setUp() {
        runeSerializer = new RuneSerializerImpl();
    }

    @ParameterizedTest(name = "{0} - {1}")
    @MethodSource("testCases")
    public void testSerializationErrorHandling(String group,
                                               String testCaseName,
                                               Class<? extends RosettaModelObject> rosettaRootType,
                                               String jsonString,
                                               String expectedErrorMessage) {
        RosettaModelObject deserializedObject = runeSerializer.deserialize(rosettaRootType, jsonString);

        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> {
            runeSerializer.serialize(deserializedObject);
        });

        assertEquals(expectedErrorMessage, runtimeException.getMessage(), testCaseName + ": Serialization error handling failed");
    }

    public static Stream<Arguments> testCases() {
        return groups(TEST_TYPE).stream()
                .flatMap(groupPath -> {
                            List<Path> rosettas = listFiles(groupPath, ".rosetta");
                            String groupName = groupPath.getFileName().toString();
                            Class<RosettaModelObject> rootDataType = generateCompileAndGetRootDataType(groupName, rosettas, helper);

                            return listFiles(groupPath, ".json").stream()
                                    .map(jsonPath -> Arguments.of(
                                            groupName,
                                            jsonPath.getFileName().toString(),
                                            rootDataType,
                                            readAsString(jsonPath),
                                            readAsString(toErrorFile(jsonPath))
                                    ));
                        }
                );
    }

    private static Path toErrorFile(Path jsonPath) {
        return Paths.get(jsonPath.toString().replaceAll("(.*)\\.json", "$1.error"));
    }
}
