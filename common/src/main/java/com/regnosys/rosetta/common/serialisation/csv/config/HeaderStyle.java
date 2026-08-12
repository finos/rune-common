package com.regnosys.rosetta.common.serialisation.csv.config;

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

/**
 * How a CSV header row (or, on write, the header row itself) names columns.
 */
public enum HeaderStyle {
    /**
     * Columns are named after the model attribute name, e.g. {@code firstName}.
     */
    ATTRIBUTE_NAME,
    /**
     * Columns are named after the label a {@code LabelProvider} returns for the attribute.
     * Requires a {@code LabelProvider} to be supplied; contradicts {@code hasHeader: false},
     * since a label has no meaning without a header row to carry it.
     *
     * <p>This setting is what makes a {@code SerializationFormat.CSV} transform labelled: the
     * transform factory resolves a {@code LabelProvider} when — and only when — the configuration
     * declares this style, and fails if none can be found. So it is not merely a description of a
     * file's header row; it decides how the mapper is built. See
     * {@code ClasspathTransformMapperFactory.csvMapper(String, Class, TransformRoot)}.</p>
     */
    LABEL
}
