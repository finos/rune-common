package com.regnosys.rosetta.common.serialisation.xml.deserialization;

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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.regnosys.rosetta.common.serialisation.xml.config.AttributeXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.AttributeXMLRepresentation;
import com.regnosys.rosetta.common.serialisation.xml.config.TypeXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.XMLContentModel;
import com.regnosys.rosetta.common.serialisation.xml.config.XMLContentModelNodeType;
import com.regnosys.rosetta.common.serialisation.xml.deserialization.RoutingInput.Namespace;
import com.regnosys.rosetta.common.serialisation.xml.deserialization.VirtualPathBuilderHelper.BufferedLeaf;
import com.regnosys.rosetta.common.serialisation.xml.deserialization.VirtualPathBuilderHelper.RoutedAssignment;
import com.regnosys.rosetta.common.serialisation.xml.deserialization.XMLContentModelMatcher.OccurrenceKey;
import com.regnosys.rosetta.common.serialisation.xml.deserialization.XMLContentModelMatcher.RoutingResult;
import com.rosetta.model.lib.RosettaModelObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.stream.XMLStreamReader;

/**
 * Jackson deserializer wrapper that resolves ambiguous XML element names by routing them to the
 * correct Rosetta property path before delegating to the standard Jackson bean deserializer.
 *
 * <p>The wrapper buffers exactly the current XML object's fields, runs the matcher to determine
 * the routing assignment for fields whose XML element name appears in the configured content
 * model, emits a rewritten token stream where nested routed fields are removed, lets the delegate
 * deserializer produce the parent Rosetta object, then attaches the routed virtual sub-graph by
 * mutating the result via {@code toBuilder()}.</p>
 */
final class XMLContentModelDisambiguatingDeserializer extends DelegatingDeserializer {

    private static final long serialVersionUID = 1L;

    private final Class<?> beanClass;
    private final TypeXMLConfiguration typeConfig;
    private final XMLContentModel contentModel;
    private final Set<String> routedXmlNames;
    private final boolean hasAnyNode;
    private final Set<String> attributeNames;
    private final VirtualPathBuilderHelper virtualPathBuilderHelper;

    XMLContentModelDisambiguatingDeserializer(JsonDeserializer<?> delegatee,
                                              Class<?> beanClass,
                                              TypeXMLConfiguration typeConfig,
                                              XMLContentModel contentModel,
                                              VirtualPathBuilderHelper virtualPathBuilderHelper) {
        super(delegatee);
        this.beanClass = beanClass;
        this.typeConfig = typeConfig;
        this.contentModel = contentModel;
        this.routedXmlNames = collectXmlNames(contentModel);
        this.hasAnyNode = containsAnyNode(contentModel);
        this.attributeNames = collectXmlAttributeNames(typeConfig);
        this.virtualPathBuilderHelper = virtualPathBuilderHelper;
    }

    @Override
    protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
        return new XMLContentModelDisambiguatingDeserializer(newDelegatee, beanClass, typeConfig, contentModel, virtualPathBuilderHelper);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (!p.hasToken(JsonToken.START_OBJECT)) {
            return _delegatee.deserialize(p, ctxt);
        }

        List<BufferedField> fields = readCurrentObject(p, ctxt);

        // Build the routing input list. Named models only need mentioned XML names; models with
        // ANY must see all non-attribute child elements so the wildcard has something to consume.
        List<RoutingInput> inputs = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            BufferedField field = fields.get(i);
            if (field.xmlAttribute) {
                continue;
            }
            if (!hasAnyNode && !routedXmlNames.contains(field.xmlName)) {
                continue;
            }
            inputs.add(new RoutingInput(i, field.xmlName, field.namespace));
        }

        RoutingResult routing = XMLContentModelMatcher.route(contentModel, inputs);

        if (routing.getStatus() == RoutingResult.Status.NO_MATCH) {
            throw JsonMappingException.from(p, formatNoMatchError(inputs));
        }
        if (routing.getStatus() == RoutingResult.Status.AMBIGUOUS) {
            throw JsonMappingException.from(p, formatAmbiguousError(inputs, routing));
        }

        Map<Integer, List<String>> routesByIndex = routing.getPathByIndex();
        Map<Integer, OccurrenceKey> occurrencesByIndex = routing.getOccurrenceByIndex();

        // Collect nested routed assignments (those whose path has more than one segment).
        // routesByIndex keys are input indexes (filtered list); translate to original field indexes.
        List<RoutedAssignment> nestedAssignments = new ArrayList<>();
        Set<Integer> nestedIndexes = new HashSet<>();
        for (Map.Entry<Integer, List<String>> entry : routesByIndex.entrySet()) {
            if (entry.getValue().size() > 1) {
                int inputIdx = entry.getKey();
                int fieldIdx = inputs.get(inputIdx).getFieldIndex();
                nestedIndexes.add(fieldIdx);
                BufferedField field = fields.get(fieldIdx);
                String occurrenceKey = String.valueOf(occurrencesByIndex.get(inputIdx));
                nestedAssignments.add(new RoutedAssignment(entry.getValue(), occurrenceKey,
                        new BufferedLeaf(field.value)));
            }
        }

        // Build the rewritten token stream that excludes the nested routed fields.
        TokenBuffer rewritten = new TokenBuffer(ctxt.getParser(), ctxt);
        rewritten.writeStartObject();
        for (int i = 0; i < fields.size(); i++) {
            if (nestedIndexes.contains(i)) {
                continue;
            }
            BufferedField field = fields.get(i);
            rewritten.writeFieldName(field.xmlName);
            try (JsonParser valueParser = field.value.asParserOnFirstToken()) {
                rewritten.copyCurrentStructure(valueParser);
            }
        }
        rewritten.writeEndObject();

        Object result;
        try (JsonParser rewrittenParser = rewritten.asParserOnFirstToken()) {
            result = _delegatee.deserialize(rewrittenParser, ctxt);
        }

        if (!nestedAssignments.isEmpty()) {
            result = applyVirtualAssignments(result, nestedAssignments, ctxt);
        }
        return result;
    }

    private Object applyVirtualAssignments(Object result,
                                           List<RoutedAssignment> assignments,
                                           DeserializationContext ctxt) throws IOException {
        if (!(result instanceof RosettaModelObject)) {
            throw new IOException("XML content-model routing on " + beanClass.getName()
                    + " requires a RosettaModelObject result, got "
                    + (result == null ? "null" : result.getClass().getName()));
        }
        RosettaModelObject obj = (RosettaModelObject) result;
        Object builder = obj.toBuilder();
        virtualPathBuilderHelper.applyAssignments(builder, assignments, ctxt);
        try {
            Object built = builder.getClass().getMethod("build").invoke(builder);
            return built;
        } catch (ReflectiveOperationException e) {
            throw new IOException("Failed to rebuild " + obj.getClass().getName()
                    + " after applying virtual assignments", e);
        }
    }

    private List<BufferedField> readCurrentObject(JsonParser p, DeserializationContext ctxt) throws IOException {
        List<BufferedField> fields = new ArrayList<>();
        JsonToken token = p.nextToken();
        while (token == JsonToken.FIELD_NAME) {
            String xmlName = p.currentName();
            Namespace namespace = currentNamespace(p);
            JsonToken valueToken = p.nextToken();
            if (valueToken == null) {
                throw JsonMappingException.from(p, "Unexpected end of XML object while reading "
                        + beanClass.getName() + "." + xmlName);
            }
            TokenBuffer value = new TokenBuffer(p, ctxt);
            value.copyCurrentStructure(p);
            boolean isAttribute = attributeNames.contains(xmlName);
            fields.add(new BufferedField(xmlName, namespace, isAttribute, value));
            token = p.nextToken();
        }
        if (token != JsonToken.END_OBJECT) {
            throw JsonMappingException.from(p, "Expected end of XML object for "
                    + beanClass.getName() + " but found " + token);
        }
        return fields;
    }

    private Namespace currentNamespace(JsonParser p) {
        if (p instanceof FromXmlParser) {
            XMLStreamReader reader = ((FromXmlParser) p).getStaxReader();
            if (reader != null) {
                String namespace = reader.getNamespaceURI();
                if (namespace != null && !namespace.isEmpty()) {
                    return Namespace.present(namespace);
                }
                return Namespace.absent();
            }
            return Namespace.unknown();
        }
        return Namespace.unknown();
    }

    private String formatNoMatchError(List<RoutingInput> inputs) {
        StringBuilder sb = new StringBuilder();
        sb.append("Cannot route XML content for ").append(beanClass.getName()).append(".\n");
        sb.append("XML child sequence: ").append(xmlSequence(inputs)).append("\n");
        sb.append("Configured content model did not match all routed child elements.");
        return sb.toString();
    }

    private String formatAmbiguousError(List<RoutingInput> inputs, RoutingResult routing) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ambiguous XML content for ").append(beanClass.getName()).append(".\n");
        sb.append("XML child sequence: ").append(xmlSequence(inputs)).append("\n");
        sb.append("Remaining candidate paths:");
        for (List<List<String>> candidate : routing.getCandidatePaths()) {
            sb.append("\n- ");
            for (int i = 0; i < candidate.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(joinPath(candidate.get(i)));
            }
        }
        return sb.toString();
    }

    private static String xmlSequence(List<RoutingInput> inputs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < inputs.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(inputs.get(i).getXmlName());
        }
        return sb.toString();
    }

    private static String joinPath(List<String> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) sb.append(".");
            sb.append(path.get(i));
        }
        return sb.toString();
    }

    private static Set<String> collectXmlNames(XMLContentModel root) {
        Set<String> names = new LinkedHashSet<>();
        collectXmlNamesRecursive(root, names);
        return names;
    }

    private static void collectXmlNamesRecursive(XMLContentModel node, Set<String> names) {
        if (node.getNodeType() == XMLContentModelNodeType.ELEMENT) {
            node.getXmlName().ifPresent(names::add);
        }
        node.getChildren().ifPresent(children -> {
            for (XMLContentModel child : children) {
                collectXmlNamesRecursive(child, names);
            }
        });
    }

    private static boolean containsAnyNode(XMLContentModel root) {
        final boolean[] result = {false};
        walk(root, node -> {
            if (node.getNodeType() == XMLContentModelNodeType.ANY) {
                result[0] = true;
            }
        });
        return result[0];
    }

    private static Set<String> collectXmlAttributeNames(TypeXMLConfiguration typeConfig) {
        Set<String> names = new HashSet<>();
        if (typeConfig == null) {
            return names;
        }
        Optional<Map<String, AttributeXMLConfiguration>> attrs = typeConfig.getAttributes();
        if (!attrs.isPresent()) {
            return names;
        }
        for (Map.Entry<String, AttributeXMLConfiguration> entry : attrs.get().entrySet()) {
            AttributeXMLConfiguration cfg = entry.getValue();
            if (cfg.getXmlRepresentation()
                    .filter(rep -> rep == AttributeXMLRepresentation.ATTRIBUTE)
                    .isPresent()) {
                String xmlName = cfg.getXmlName().orElse(entry.getKey());
                names.add(xmlName);
            }
        }
        return names;
    }

    /**
     * Returns {@code true} when the supplied content model requires any disambiguation work at all.
     * If every path is direct and single-segment AND the model contains no duplicate XML names, the
     * default Jackson routing already produces the correct result and the wrapper can be skipped.
     */
    static boolean requiresRouting(XMLContentModel contentModel) {
        Set<String> seen = new HashSet<>();
        boolean[] hasNested = {false};
        boolean[] hasDuplicate = {false};
        walk(contentModel, node -> {
            if (node.getNodeType() == XMLContentModelNodeType.ELEMENT
                    || node.getNodeType() == XMLContentModelNodeType.ANY) {
                node.getPath().ifPresent(path -> {
                    if (path.size() > 1) {
                        hasNested[0] = true;
                    }
                });
                node.getXmlName().ifPresent(name -> {
                    if (!seen.add(name)) {
                        hasDuplicate[0] = true;
                    }
                });
            }
        });
        return hasNested[0] || hasDuplicate[0];
    }

    private static void walk(XMLContentModel node, java.util.function.Consumer<XMLContentModel> visitor) {
        visitor.accept(node);
        node.getChildren().ifPresent(children -> {
            for (XMLContentModel child : children) {
                walk(child, visitor);
            }
        });
    }

    /**
     * Buffered representation of one XML child field in the parent object.
     */
    static final class BufferedField {
        final String xmlName;
        final Namespace namespace;
        final boolean xmlAttribute;
        final TokenBuffer value;

        BufferedField(String xmlName, Namespace namespace, boolean xmlAttribute,
                      TokenBuffer value) {
            this.xmlName = xmlName;
            this.namespace = namespace;
            this.xmlAttribute = xmlAttribute;
            this.value = value;
        }
    }
}
