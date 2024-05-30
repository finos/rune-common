package com.regnosys.rosetta.common.util;

/*-
 * #%L
 * Rune Common
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class PathLoader {

    public static Stream<Path> loadFromClasspath(String path, ClassLoader classLoader) {
        List<Path> paths = new ArrayList<>();
        try {
            for (URL resource : Collections.list(classLoader.getResources(path))) {
                if (resource.toURI().getScheme().equals("jar")) {
                    try {
                        FileSystems.getFileSystem(resource.toURI());
                    } catch (FileSystemNotFoundException e) {
                        FileSystems.newFileSystem(resource.toURI(), Collections.emptyMap());
                    }
                }
                paths.add(Paths.get(resource.toURI()));
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
        return paths.stream();
    }

}
