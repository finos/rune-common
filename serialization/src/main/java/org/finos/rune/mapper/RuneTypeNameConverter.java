package org.finos.rune.mapper;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2026 REGnosys
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

import javax.lang.model.SourceVersion;
import java.util.function.UnaryOperator;

/**
 * Converts between the qualified name of a Rune type and the name of the Java class generated for it,
 * for the serializers and the type id resolver that write and read {@code @type}, which holds the Rune name.
 * <p>
 * The generator puts {@code _} before each namespace segment that is a Java keyword, so namespace
 * {@code a.new.b} is generated as package {@code a._new.b}. The generated class does not record its Rune
 * namespace, so a namespace segment that is itself {@code _new} cannot be told from {@code new}, and
 * {@link #toRuneName(String)} gives {@code new}.
 */
public final class RuneTypeNameConverter {

    private static final String ESCAPE_PREFIX = "_";

    private static final ClassValue<String> RUNE_NAMES = new ClassValue<String>() {
        @Override
        protected String computeValue(Class<?> javaClass) {
            return toRuneName(javaClass.getName());
        }
    };

    private RuneTypeNameConverter() {
    }

    public static String toRuneName(Class<?> javaClass) {
        return RUNE_NAMES.get(javaClass);
    }

    /** {@code a._new.b.Root} becomes {@code a.new.b.Root}. */
    public static String toRuneName(String javaClassName) {
        return mapPackageSegments(javaClassName, RuneTypeNameConverter::unescape);
    }

    /** {@code a.new.b.Root} becomes {@code a._new.b.Root}. */
    public static String toJavaClassName(String runeName) {
        return mapPackageSegments(runeName, RuneTypeNameConverter::escape);
    }

    private static String mapPackageSegments(String qualifiedName, UnaryOperator<String> segmentMapper) {
        String[] segments = qualifiedName.split("\\.", -1);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                result.append('.');
            }
            boolean isPackageSegment = i < segments.length - 1;
            result.append(isPackageSegment ? segmentMapper.apply(segments[i]) : segments[i]);
        }
        return result.toString();
    }

    private static String escape(String segment) {
        String escaped = segment;
        while (isReservedWord(escaped)) {
            escaped = ESCAPE_PREFIX + escaped;
        }
        return escaped;
    }

    private static String unescape(String segment) {
        String unescaped = segment;
        while (unescaped.startsWith(ESCAPE_PREFIX) && isReservedWord(unescaped.substring(ESCAPE_PREFIX.length()))) {
            unescaped = unescaped.substring(ESCAPE_PREFIX.length());
        }
        return unescaped;
    }

    /** True for an identifier Java reserves, such as {@code new}, {@code true} or {@code _}. */
    private static boolean isReservedWord(String segment) {
        return SourceVersion.isIdentifier(segment) && !SourceVersion.isName(segment);
    }
}
