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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RuneSerializerTest {
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
    public void testSerializationRoundTrip(String group, String testCaseName, Class<? extends RosettaModelObject> rosettaRootType, String jsonString) {
        RosettaModelObject deserializedObject = runeSerializer.fromJson(rosettaRootType, jsonString);
        String serializedjsonString = runeSerializer.toJson(deserializedObject);
        assertEquals(jsonString, serializedjsonString, testCaseName + ": Serialization round trip failed");
    }

    public static Stream<Arguments> testCases() throws IOException {
        return groups().stream()
                .flatMap(groupPath -> {
                            List<Path> rosettas = listFiles(groupPath, ".rosetta");
                            String groupName = groupPath.getFileName().toString();
                            Class<RosettaModelObject> rootDataType = generateCompileAndGetRootDataType(groupName, rosettas);

                            return listFiles(groupPath, ".json").stream()
                                    .map(jsonPath -> Arguments.of(
                                            groupName,
                                            jsonPath.getFileName().toString(),
                                            rootDataType,
                                            readAsString(jsonPath)
                                    ));
                        }
                );
    }

    private static <T extends RosettaModelObject> Class<T> generateCompileAndGetRootDataType(String groupName, List<Path> rosettaPaths) {
        String[] rosettaFileContents = rosettaPaths.stream().map(RuneSerializerTest::readAsString).toArray(String[]::new);
        HashMap<String, String> generatedCode = helper.generateCode(rosettaFileContents);
        Map<String, Class<?>> compiledCode = helper.compileToClasses(generatedCode);
        Class<?> aClass = compiledCode.get(groupName + ".Root");
        return (Class<T>) aClass;
    }

    private static String readAsString(Path jsonPath) {
        try {
            return new String(Files.readAllBytes(jsonPath));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Path> groups() {
        try (Stream<Path> files = Files.list(Paths.get("src/test/resources/rune-serializer-test"))) {
            return files.filter(Files::isDirectory).collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Path> listFiles(Path groupPath, String suffix) {
        try (Stream<Path> files = Files.list(groupPath)) {
            return files.filter(x -> x.getFileName().toString().endsWith(suffix)).collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}