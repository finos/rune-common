package com.regnosys.rosetta.common.serialisation.fix;

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

import com.regnosys.rosetta.common.transform.LabelProviderResolver;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.model.lib.path.RosettaPath;
import com.regnosys.rosetta.common.serialisation.fix.processor.RuneFIXSerializerProcessor;
import com.regnosys.rosetta.common.serialisation.fix.processor.RuneFIXSerializerReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.DataDictionary;
import quickfix.InvalidMessage;
import quickfix.Message;
import quickfix.ValidationSettings;

/**
 * Primary orchestrator for converting Rune domain models into QuickFIX/J messages and FIX-formatted strings.
 *
 * <p>Acting as the central entry point for FIX serialization, this mapper coordinates the interaction
 * between domain object traversal, message routing, and metadata validation. It enforces a strict separation
 * of concerns by delegating AST processing to the lightweight {@link RuneFIXSerializerProcessor} while
 * managing high-level configuration rules and dictionary operations natively.</p>
 *
 * <p>Core responsibilities include:</p>
 * <ul>
 *   <li><b>Routing & Orchestration:</b> Dynamically resolves the appropriate FIX MsgType (Tag 35) for a given {@link RosettaModelObject} using the rules defined in the {@link RuneFIXConfiguration}.</li>
 *   <li><b>Serialization:</b> Initializes isolated processor instances to translate hierarchical domain models into flat, SOH-delimited FIX payloads or programmatic QuickFIX/J {@link Message} objects.</li>
 *   <li><b>Validation:</b> Each write operation can optionally validate the generated transactional FIX body against the application {@link DataDictionary}. Validation diagnostics are returned through {@link RuneFIXSerializerReport} and are disabled by default.
 * </li>
 * </ul>
 *
 * <p><b>Usage Note:</b> The mapper is structurally immutable and thread-safe. It is designed to be instantiated via its static factory method on application startup and safely reused across concurrent serialization requests. Deserialization workflows ({@code readValue}) are currently stubbed pending future implementation.</p>
 */
public class RuneFIXMapper {

    private static final Logger logger = LoggerFactory.getLogger(RuneFIXMapper.class);

    private final RuneFIXConfiguration config;
    private final DataDictionary dictionary;

    private RuneFIXMapper(RuneFIXConfiguration config, DataDictionary dictionary) {
        this.config = config;
        this.dictionary = dictionary;
    }

    public static RuneFIXMapper createFIXMapper(RuneFIXConfiguration config, DataDictionary dictionary) {
        if (config == null) {
            throw new IllegalArgumentException("RuneFIXConfiguration is required to resolve MsgTypes. Cannot be null.");
        }
        if (dictionary == null) {
            throw new IllegalArgumentException("DataDictionary is required for FIX metadata resolution. Cannot be null.");
        }
        return new RuneFIXMapper(config, dictionary);
    }

    public RuneFIXSerializerReport writeValueAsFIXReport(Object value, boolean validate) {
        if (value == null) return null;
        if (!(value instanceof RosettaModelObject)) {
            throw new IllegalArgumentException("RuneFIXMapper requires a rune model object, not " + value.getClass().getName());
        }

        RosettaModelObject instance = (RosettaModelObject) value;
        Class<? extends RosettaModelObject> valueType = instance.getType();

        LabelProvider activeLabelProvider = LabelProviderResolver.fromType(valueType);
        String msgType = config.getMsgTypeFor(valueType);
        RosettaPath rootPath = RosettaPath.valueOf(valueType.getSimpleName());

        RuneFIXSerializerProcessor processor = new RuneFIXSerializerProcessor(dictionary, activeLabelProvider, msgType);
        instance.process(rootPath, processor);
        return processor.report(validate);
    }

    public RuneFIXSerializerReport writeValueAsFIXReport(Object value) {
        return writeValueAsFIXReport(value, false);
    }

    public Message writeValueAsFIXMessage(Object value, boolean validate) {
        RuneFIXSerializerReport report = writeValueAsFIXReport(value, validate);
        return report == null ? null : report.getFixMessage();
    }

    public Message writeValueAsFIXMessage(Object value) {
        return writeValueAsFIXMessage(value, false);
    }

    public String writeValueAsString(Object value, boolean validate) {
        Message fixMessage = writeValueAsFIXMessage(value, validate);
        return fixMessage != null ? fixMessage.toString() : null;
    }

    public String writeValueAsString(Object value) {
        return writeValueAsString(value, false);
    }

    public <T extends RosettaModelObject> T readValue(String content, Class<T> valueType) throws IllegalArgumentException {
        try {
            Message fixMessage = new Message();
            fixMessage.fromString(content, dictionary, new ValidationSettings(), false);
            return readValueFromFIXMessage(fixMessage, valueType);
        } catch (InvalidMessage e) {
            logger.error("Failed to parse FIX string content into a valid QuickFIX/J Message.", e);
            throw new IllegalArgumentException("Malformed FIX string provided to readValue.", e);
        }
    }

    public <T extends RosettaModelObject> T readValueFromFIXMessage(Message message, Class<T> valueType) {
        throw new UnsupportedOperationException("FIX to Rune deserialization is pending implementation.");
    }
}
