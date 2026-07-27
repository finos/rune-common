package com.regnosys.rosetta.common.util;

/*-
 * ==============
 * Rune Common
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

import java.util.Optional;

/**
 * Line ending normalisation for content that is written on one operating system
 * and compared on another.
 * <p>
 * Serialised documents are pinned to "\n" when they are produced, but expectation
 * files committed to a repository without a {@code .gitattributes} still check out
 * with CRLF on Windows. Comparing produced output against such a file therefore has
 * to normalise both sides, otherwise the comparison comes down to which operating
 * system the file was checked out on.
 */
public class LineEndings {

    private LineEndings() {
    }

    /**
     * Converts CRLF and lone CR line endings to "\n". Returns null for null input.
     */
    public static String normalise(String str) {
        return Optional.ofNullable(str)
                .map(s -> s.replace("\r\n", "\n").replace("\r", "\n"))
                .orElse(null);
    }
}
