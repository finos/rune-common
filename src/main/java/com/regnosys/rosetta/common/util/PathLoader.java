package com.regnosys.rosetta.common.util;

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
