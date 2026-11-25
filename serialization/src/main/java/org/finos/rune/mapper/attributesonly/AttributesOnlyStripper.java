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

import java.util.Iterator;
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
 * <p>
 * {@link AttributesOnlySerializerProvider} applies this to each root value an attributes-only mapper writes,
 * so the input is always the whole standard Rune JSON tree, never a fragment of it.
 */
public final class AttributesOnlyStripper {

    private static final String META_PREFIX = "@";

    private AttributesOnlyStripper() {
    }

    /**
     * @param root a Rune JSON tree, as written by a standard {@code RuneJsonObjectMapper}; it is modified in
     *             place, so pass a tree nothing else holds
     * @return the tree holding only model attributes: {@code root} itself, a node within it, or an empty
     *         container when nothing is left
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
        if (node.isEmpty()) {
            return node;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.properties().iterator();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode value = field.getKey().startsWith(META_PREFIX) ? null : stripNode(field.getValue());
            if (value == null) {
                fields.remove();
            } else {
                field.setValue(value);
            }
        }
        return node.isEmpty() ? null : node;
    }

    private static JsonNode stripArray(ArrayNode node) {
        if (node.isEmpty()) {
            return node;
        }
        for (int i = node.size() - 1; i >= 0; i--) {
            JsonNode value = stripNode(node.get(i));
            if (value == null) {
                node.remove(i);
            } else {
                node.set(i, value);
            }
        }
        return node.isEmpty() ? null : node;
    }
}
