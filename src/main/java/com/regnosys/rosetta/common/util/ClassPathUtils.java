package com.regnosys.rosetta.common.util;

import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassPathUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassPathUtils.class);

    /**
     * Searches the class path for given directories and returns all files contained in the directory and sub directories filtered by the regexes.
     * @param classPathDirectories List of all the directories to search for in the class path loader.
     * @param includeRegex Regex file pattern to search files on.
     * @param excludeRegex Regex file pattern to exclude files on.
     * @param classLoader Classloader to search in.
     * @return
     */

    public static List<Path> findPathsFromClassPath(Collection<String> classPathDirectories, String includeRegex, Optional<String> excludeRegex, ClassLoader classLoader) {
        List<Path> modelPaths = classPathDirectories.stream().flatMap(path -> loadFromClasspath(path, classLoader))
                .collect(Collectors.toList());
        List<Path> expandedModelPaths = expandPaths(modelPaths, includeRegex, excludeRegex);
        LOGGER.info("Using paths:" + expandedModelPaths);
        expandedModelPaths.forEach(x -> LOGGER.debug("   " + x));
        return expandedModelPaths;
    }

    public static List<Path> findRosettaFilePaths() {
        return findPathsFromClassPath(ImmutableList.of("model", "cdm/rosetta"), ".*\\.rosetta", Optional.empty(), ClassPathUtils.class.getClassLoader());
    }

    /**
     * Includes basictypes.rosetta and annotations.rosetta
     */
    public static List<Path> findStaticRosettaFilePaths() {
        return ClassPathUtils.findPathsFromClassPath(ImmutableList.of("model"), ".*\\.rosetta", Optional.empty(), ClassPathUtils.class.getClassLoader());
    }

    public static Path toPath(URL resource) {
        try {
            return Paths.get(resource.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error converting resource url to path " + resource);
        }
    }

    public static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error converting resource path to url " + path);
        }
    }

    public static List<Path> expandPaths(List<Path> paths, String includeRegex, Optional<String> excludeRegex) {
        return paths.stream().flatMap(ClassPathUtils::listFiles)
                .filter(p -> p.getFileName().toString().matches(includeRegex))
                .filter(p -> !excludeRegex.isPresent() || !p.getFileName().toString().matches(excludeRegex.get()))
                .collect(Collectors.toList());
    }
    
    public static Stream<Path> loadFromClasspath(String path) {
        return loadFromClasspath(path, ClassPathUtils.class.getClassLoader());
    }

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
                paths.add(toPath(resource));
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
        return paths.stream();
    }
    
    public static Path loadSingleFromClasspath(String path) {
        return loadSingleFromClasspath(path, ClassPathUtils.class.getClassLoader());
    }
    
    public static Path loadSingleFromClasspath(String path, ClassLoader classLoader) {
        return toPath(classLoader.getResource(path));
    }

    private static List<Path> pathsExist(List<Path> modelPaths) {
        List<Path> nonExistentPaths = modelPaths.stream().filter(p -> !Files.exists(p)).collect(Collectors.toList());
        if (!nonExistentPaths.isEmpty()) {
            throw new IllegalArgumentException("Paths " + nonExistentPaths + " do not exist.");
        }
        return modelPaths;
    }

    private static Stream<Path> listFiles(Path path) {
        try {
            return Files.walk(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


}
