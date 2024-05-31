package com.regnosys.rosetta.common.util;

/*-
 * ==============
 * Rosetta Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
 * ==============
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

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassPathUtilsTest {

    private static ClassLoader CLASS_LOADER;

    @BeforeAll
    static void setUp() {
        CLASS_LOADER = ClassPathUtils.class.getClassLoader();
    }

    @DisplayName("Find all .rosetta files in class path directory")
    @Test
    void shouldFindAllFilesInDirectory() {
        List<Path> pathsFromClassPath = ClassPathUtils.findPathsFromClassPath(
                ImmutableList.of("path-files"), ".*\\.rosetta", Optional.empty(), CLASS_LOADER);
        assertThat(pathsFromClassPath.stream().map(Path::toString).collect(Collectors.toList()),
                hasItems(matchesPattern(".*test-path.rosetta"), matchesPattern(".*test-path-2.rosetta")));
    }

    @DisplayName("No regex provided")
    @Test
    void shouldFindNoFiles() {
        List<Path> pathsFromClassPath = ClassPathUtils.findPathsFromClassPath(
                ImmutableList.of(""), "", Optional.empty(), CLASS_LOADER);
        assertEquals(0, pathsFromClassPath.size());
    }

    @DisplayName("Typo in regex")
    @Test
    void shouldFindNoFilesMatchingRegex() {
        List<Path> pathsFromClassPath = ClassPathUtils.findPathsFromClassPath(
                ImmutableList.of("path-files"), ".*\\.roetta", Optional.empty(), CLASS_LOADER);
        assertEquals(0, pathsFromClassPath.size());
    }

    @DisplayName(".rosetta files excluded from regex")
    @Test
    void shouldFindNoFilesWithRegexExclusion() {
        List<Path> pathsFromClassPath = ClassPathUtils.findPathsFromClassPath(
                ImmutableList.of("path-files"), "", Optional.of(".*\\.rosetta"), CLASS_LOADER);
        assertEquals(0, pathsFromClassPath.size());
    }
}
