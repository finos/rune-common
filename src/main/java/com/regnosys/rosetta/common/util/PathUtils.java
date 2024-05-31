package com.regnosys.rosetta.common.util;

/*-
 * ==============
 * Rosetta Common
 * --------------
 * Copyright (C) 2018 - 2024 REGnosys
 * --------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============
 */

/*-
 * #%L
 * Rosetta Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.common.io.Resources;
import com.regnosys.rosetta.common.translation.Path;
import com.rosetta.model.lib.path.RosettaPath;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PathUtils {

    public static RosettaPath toRosettaPath(Path path) {
        List<RosettaPath.Element> elements = path.getElements().stream()
                .map(x -> RosettaPath.Element.create(x.getPathName(), toOptionalInt(x.getIndex()), x.getMetas()))
                .collect(Collectors.toList());
        return RosettaPath.createPathFromElements(elements);
    }

    public static Path toPath(RosettaPath rosettaPath) {
        List<Path.PathElement> pathElements = rosettaPath.allElements().stream()
                .map(x -> new Path.PathElement(x.getPath(), toOptionalInteger(x.getIndex()), x.getMetas()))
                .collect(Collectors.toList());
        return new Path(pathElements);
    }

    private static OptionalInt toOptionalInt(Optional<Integer> i) {
        return i.map(OptionalInt::of).orElse(OptionalInt.empty());
    }

    private static Optional<Integer> toOptionalInteger(OptionalInt i) {
        return i.isPresent() ? Optional.of(i.getAsInt()) : Optional.empty();
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

    /**
     * Filter sub-paths (e.g. that endWith other paths) from list of paths.
     * e.g. given list ["a.b.c", "b.c", "c", "a.b", "x.y.z"] would be filtered to ["a.b.c", "a.b", "x.y.z"].
     */
    public static List<Path> filterSubPaths(Collection<Path> paths) {
        return paths.stream()
                .filter(path -> paths.stream()
                        .filter(p -> !p.nameIndexMatches(path)) // do not compare against itself
                        .noneMatch(p -> p.endsWith(path)))
                .collect(Collectors.toList());
    }


}
