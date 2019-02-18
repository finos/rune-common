package com.regnosys.rosetta.common.util;

import com.google.common.io.Resources;
import com.regnosys.rosetta.common.translation.Path;
import com.regnosys.rosetta.common.translation.Path.PathElement;
import com.rosetta.model.lib.path.RosettaPath;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PathUtils {

    public static RosettaPath toRosettaPath(Path path) {
        List<RosettaPath.Element> collect = path.getElements().stream()
                .map(x -> RosettaPath.Element.create(x.getPathName(), toOptionalInt(x), x.getMetas()))
                .collect(Collectors.toList());

        return RosettaPath.createPathFromElements(collect);
    }

    private static OptionalInt toOptionalInt(PathElement x) {
        return x.getIndex().map(OptionalInt::of).orElse(OptionalInt.empty());
    }

    /**
     * Parse contents of excluded paths file into list of path objects.
     */
    public static List<Path> getExcludedPaths(String excludedPathsFile) {
        return Optional.ofNullable(excludedPathsFile)
                .map(PathUtils::parseExcludedPathsFile)
                .orElse(Collections.emptyList());
    }

    private static List<Path> parseExcludedPathsFile(String excludedPathsFile) {
        URL resource = Resources.getResource(excludedPathsFile);
        try (Stream<String> stream = Files.lines(Paths.get(resource.toURI()))) {
            return stream.map(Path::parse).collect(Collectors.toList());
        } catch (IOException |URISyntaxException e) {
            throw new PathException("Unable to load excluded xml paths", e);
        }
    }
}