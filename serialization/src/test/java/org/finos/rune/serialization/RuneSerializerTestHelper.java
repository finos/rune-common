package org.finos.rune.serialization;

import com.regnosys.rosetta.tests.util.CodeGeneratorTestHelper;
import com.rosetta.model.lib.RosettaModelObject;

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

public class RuneSerializerTestHelper {

    @SuppressWarnings("unchecked")
    public static <T extends RosettaModelObject> Class<T> generateCompileAndGetRootDataType(String groupName,
                                                                                            List<Path> rosettaPaths,
                                                                                            CodeGeneratorTestHelper helper) {
        String[] rosettaFileContents = rosettaPaths.stream().map(RuneSerializerTestHelper::readAsString).toArray(String[]::new);
        HashMap<String, String> generatedCode = helper.generateCode(rosettaFileContents);
        Map<String, Class<?>> compiledCode = helper.compileToClasses(generatedCode);
        Class<?> aClass = compiledCode.get(groupName + ".Root");
        return (Class<T>) aClass;
    }

    public static String readAsString(Path jsonPath) {
        try {
            return new String(Files.readAllBytes(jsonPath));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<Path> groups(String testType) {
        try (Stream<Path> files = Files.list(Paths.get("src/test/resources/" + testType))) {
            return files.filter(Files::isDirectory).collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Path getFile(Path groupPath, String fileName) {
        try (Stream<Path> files = Files.list(groupPath)) {
            return files.filter(x -> x.getFileName().toString().equals(fileName)).collect(Collectors.toList())
                    .stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("File not found: " + fileName));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<Path> listFiles(Path groupPath, String suffix) {
        try (Stream<Path> files = Files.list(groupPath)) {
            return files.filter(x -> x.getFileName().toString().endsWith(suffix)).collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
