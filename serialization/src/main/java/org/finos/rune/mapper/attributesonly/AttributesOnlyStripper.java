package org.finos.rune.mapper.attributesonly;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.finos.rune.mapper.RuneJsonConfig;

import java.util.Map;

/**
 * Turns a Rune JSON tree into one that holds only model attributes.
 * <ul>
 *   <li>Every {@code @}-prefixed property is removed: type information ({@code @model}, {@code @type},
 *       {@code @version}) and metadata ({@code @key}, {@code @ref}, {@code @scheme} and their
 *       {@code :scoped}/{@code :external} forms). A Rune attribute name cannot start with {@code @}, so no
 *       attribute is removed by this rule.</li>
 *   <li>An object holding {@code @data} (a basic-type choice option, or a value with metadata) is replaced
 *       by its {@code @data} value.</li>
 *   <li>An object or array that held something and is left empty by the rules above is omitted. This is
 *       how an attribute held only as a reference ({@code {"@ref:external": "..."}}) disappears, along with
 *       any object or list that held nothing but such references.</li>
 * </ul>
 * The root is never omitted: a root left empty is written as an empty object or array, matching its type.
 */
public final class AttributesOnlyStripper {

    private static final String META_PREFIX = "@";

    private AttributesOnlyStripper() {
    }

    /**
     * @param root a Rune JSON tree, as written by a standard {@code RuneJsonObjectMapper}
     * @return a new tree holding only model attributes; {@code root} is not modified
     */
    public static JsonNode strip(JsonNode root) {
        JsonNode stripped = stripNode(root);
        if (stripped != null) {
            return stripped;
        }
        return root.isArray() ? JsonNodeFactory.instance.arrayNode() : JsonNodeFactory.instance.objectNode();
    }

    /**
     * @return the stripped node, or {@code null} when stripping left a non-empty container empty
     */
    private static JsonNode stripNode(JsonNode node) {
        if (node.isObject()) {
            return stripObject((ObjectNode) node);
        }
        if (node.isArray()) {
            return stripArray((ArrayNode) node);
        }
        return node;
    }

    private static JsonNode stripObject(ObjectNode node) {
        JsonNode data = node.get(RuneJsonConfig.MetaProperties.DATA);
        if (data != null) {
            return stripNode(data);
        }
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        for (Map.Entry<String, JsonNode> field : node.properties()) {
            if (field.getKey().startsWith(META_PREFIX)) {
                continue;
            }
            JsonNode value = stripNode(field.getValue());
            if (value != null) {
                result.set(field.getKey(), value);
            }
        }
        return emptiedByStripping(node, result) ? null : result;
    }

    private static JsonNode stripArray(ArrayNode node) {
        ArrayNode result = JsonNodeFactory.instance.arrayNode();
        for (JsonNode element : node) {
            JsonNode value = stripNode(element);
            if (value != null) {
                result.add(value);
            }
        }
        return emptiedByStripping(node, result) ? null : result;
    }

    private static boolean emptiedByStripping(JsonNode original, JsonNode stripped) {
        return stripped.isEmpty() && !original.isEmpty();
    }
}
