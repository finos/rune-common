package com.regnosys.rosetta.common.serialisation.fix.processor;

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

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;

import java.util.*;

/**
 * An AST visitor that traverses a Rune domain model to dynamically construct a QuickFIX/J {@link Message} payload.
 *
 * <p>This processor translates absolute domain object paths into relative schema paths, resolving them against
 * a {@link LabelProvider} to determine the corresponding FIX label. It then queries the provided
 * {@link DataDictionary} to extract the appropriate integer tags and group metadata.</p>
 *
 * <p>Key responsibilities include:</p>
 * <ul>
 *   <li><b>Primitive Mapping:</b> Assigning scalar values directly to the active QuickFIX/J target.</li>
 *   <li><b>Repeating Groups:</b> Automatically resolving counter and delimiter tags for both complex object lists and multi-cardinality primitive lists.</li>
 *   <li><b>Hierarchy Management:</b> Maintaining an internal stack of {@link FieldMap} frames to accurately nest QuickFIX/J {@link Group} payloads within the root message as the model tree is traversed.</li>
 * </ul>
 *
 * <p><b>Note on Thread Safety:</b> This processor maintains internal state during traversal and is designed as a single-threaded operation to minimize performance overhead. A new instance must be created for each message serialization; instances should never be shared across concurrent threads.</p>
 */
public class RuneFIXSerializerProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(RuneFIXSerializerProcessor.class);

    private final DataDictionary dictionary;
    private final LabelProvider labelProvider;
    private final Message rootMessage;
    private final String msgType;

    private static class StackFrame {
        final RosettaPath path;
        final FieldMap target;
        final DataDictionary dataDictionary;

        StackFrame(RosettaPath path, FieldMap target, DataDictionary dataDictionary) {
            this.path = path;
            this.target = target;
            this.dataDictionary = dataDictionary;
        }
    }

    private final Deque<StackFrame> stack = new ArrayDeque<>();

    private final List<RuneFIXSerializerIssue> issues = new ArrayList<>();

    public RuneFIXSerializerProcessor(DataDictionary dictionary, LabelProvider labelProvider, String msgType) {
        this.dictionary = dictionary;
        this.labelProvider = labelProvider;
        this.msgType = msgType;

        this.rootMessage = new Message();
        this.rootMessage.getHeader().setString(35, msgType);

        if (logger.isDebugEnabled())
            logger.debug("Initialized RuneFIXSerializerProcessor for MsgType: {}", msgType);
    }

    /**
     * Converts an absolute object path into a schema-relative path by stripping the root message element
     * and removing all array indices using the native AST representation.
     */
    private RosettaPath toRelativePath(RosettaPath absolutePath) {
        if (absolutePath == null) {
            return null;
        }

        RosettaPath indexlessPath = absolutePath.toIndexless();
        RosettaPath relativePath = indexlessPath.depth() > 1 ? indexlessPath.trimFirst() : indexlessPath;
        String path = relativePath.buildPath();

        if (path.endsWith(".value")) {
            path = path.substring(0, path.length() - ".value".length());
        }

        if (logger.isTraceEnabled())
            logger.trace("Resolved FIX mapping path: '{}' -> '{}'", absolutePath.buildPath(), path);

        return RosettaPath.valueOf(path);
    }

    private FieldMap currentTarget(RosettaPath currentPath) {
        while (!stack.isEmpty() && !isParentPath(stack.peek().path, currentPath)) {
            StackFrame popped = stack.pop();
            if (logger.isTraceEnabled())
                logger.trace("Popped stack frame: {}", popped.path.buildPath());
        }
        return stack.isEmpty() ? rootMessage : stack.peek().target;
    }

    private boolean isParentPath(RosettaPath parent, RosettaPath child) {
        return child.buildPath().startsWith(parent.buildPath());
    }

    private DataDictionary getDataDictionaryFor(FieldMap target) {
        for (StackFrame frame : stack) {
            if (frame.target == target) {
                return frame.dataDictionary;
            }
        }
        return dictionary;
    }

    private DataDictionary.GroupInfo getGroupInfoFor(FieldMap parentTarget, int counterTag) {
        return getDataDictionaryFor(parentTarget).getGroup(msgType, counterTag);
    }

    private RuneFIXSerializerReport validateBodyOnlyReport() {

        logger.debug("Running Body-Only validation...");
        List<RuneFIXSerializerIssue> reportIssues = new ArrayList<>(issues);
        try {
            dictionary.validate(rootMessage, true, buildValidationSettings());
        } catch (Exception e) {
            RuneFIXSerializerIssue validationIssue = new RuneFIXSerializerIssue(
                    RuneFIXSerializerIssue.Severity.ERROR,
                    RuneFIXSerializerIssue.Code.FIX_VALIDATION_FAILED,
                    "",
                    e.getMessage());

            boolean alreadyReported = reportIssues.stream()
                    .anyMatch(issue ->
                            issue.getCode()
                                    .equals(validationIssue.getCode()) &&
                                    Objects.equals(issue.getMessage(), validationIssue.getMessage())
                    );

            if (!alreadyReported) {
                reportIssues.add(validationIssue);
            }
            if (logger.isWarnEnabled())
                logger.warn("Body validation failed: {}", e.getMessage());
        }

        return new RuneFIXSerializerReport(rootMessage, reportIssues);
    }

    private ValidationSettings buildValidationSettings() {
        ValidationSettings settings = new ValidationSettings();
        settings.setFirstFieldInGroupIsDelimiter(true);
        settings.setAllowUnknownMessageFields(true);
        return settings;
    }

    @Override
    public <R extends RosettaModelObject> boolean processRosetta(
            RosettaPath rosettaPath, Class<? extends R> aClass, R r,
            RosettaModelObject parent, AttributeMeta... attributeMetas) {

        if (r == null) return false;
        if (logger.isTraceEnabled())
            logger.trace("Processing complex single: {}", rosettaPath.buildPath());

        if (stack.isEmpty()) {
            stack.push(new StackFrame(rosettaPath, rootMessage, dictionary));
            if (logger.isTraceEnabled())
                logger.trace("Pushed root stack frame: {}", rosettaPath.buildPath());
        }
        return true;
    }

    @Override
    public <R extends RosettaModelObject> boolean processRosetta(
            RosettaPath rosettaPath, Class<? extends R> aClass, List<? extends R> list,
            RosettaModelObject parent, AttributeMeta... attributeMetas) {

        if (list == null || list.isEmpty()) return false;

        if (logger.isDebugEnabled())
            logger.debug("Processing repeating group at path: {} (size: {})", rosettaPath.buildPath(), list.size());

        FieldMap parentTarget = currentTarget(rosettaPath);
        RosettaPath leafPath = toRelativePath(rosettaPath);
        String counterLabel = labelProvider.getLabel(leafPath);
        if (counterLabel == null) {
            addError(RuneFIXSerializerIssue.Code.MISSING_LABEL_MAPPING, rosettaPath, String.format("No FIX label mapping found for repeating group path '%s'",
                    leafPath == null ? "" : leafPath.buildPath())
            );
            return false;
        }

        int counterTag = dictionary.getFieldTag(counterLabel);
        if (counterTag <= 0) {
            addError(RuneFIXSerializerIssue.Code.MISSING_FIX_TAG, rosettaPath, String.format("No FIX tag found for repeating group label '%s'", counterLabel));
            return false;
        }

        DataDictionary.GroupInfo groupInfo = getGroupInfoFor(parentTarget, counterTag);
        if (groupInfo == null) {
            addError(RuneFIXSerializerIssue.Code.MISSING_GROUP_METADATA, rosettaPath, String.format(
                    "No FIX group metadata found for message type '%s' and counter tag '%s'", msgType, counterTag));
            return false;
        }

        int delimiterTag = groupInfo.getDelimiterField();
        if (logger.isDebugEnabled())
            logger.debug("Resolved group '{}' -> CounterTag: {}, DelimiterTag: {}", counterLabel, counterTag, delimiterTag);

        for (int i = 0; i < list.size(); i++) {
            R item = list.get(i);
            RosettaPath itemPath = rosettaPath.withIndex(i);
            DataDictionary groupDictionary = groupInfo.getDataDictionary();
            Group group = new Group(counterTag, delimiterTag, groupDictionary.getOrderedFields());

            stack.push(new StackFrame(itemPath, group, groupDictionary));
            if (logger.isTraceEnabled())
                logger.trace("Pushed group stack frame: {}", itemPath.buildPath());

            item.process(itemPath, this);
            parentTarget.addGroup(group);
        }

        return false;
    }

    @Override
    public <T> void processBasic(
            RosettaPath rosettaPath, Class<? extends T> aClass, T t,
            RosettaModelObject parent, AttributeMeta... attributeMetas) {

        if (t == null) return;

        FieldMap target = currentTarget(rosettaPath);
        RosettaPath leafPath = toRelativePath(rosettaPath);
        String fixLabel = labelProvider.getLabel(leafPath);
        if (fixLabel == null) {
            addError(RuneFIXSerializerIssue.Code.MISSING_LABEL_MAPPING, rosettaPath,
                    String.format("No FIX label mapping found for path '%s'", leafPath == null ? "" : leafPath.buildPath())
            );
            return;
        }

        int tag = dictionary.getFieldTag(fixLabel);
        if (tag > 0) {
            DataDictionary fieldDictionary = getDataDictionaryFor(target);
            FIXSerializerFormatter.setField(target, fieldDictionary, tag, t);
            if (logger.isDebugEnabled())
                logger.debug("Set FIX field: {} (Tag: {}) = {}", fixLabel, tag, t);
        } else {
            addError(RuneFIXSerializerIssue.Code.MISSING_FIX_TAG, rosettaPath, String.format("No FIX tag found for label '%s'", fixLabel));
        }
    }

    @Override
    public <T> void processBasic(
            RosettaPath rosettaPath, Class<? extends T> aClass, Collection<? extends T> collection,
            RosettaModelObject parent, AttributeMeta... attributeMetas) {

        if (collection == null || collection.isEmpty()) return;

        if (logger.isDebugEnabled())
            logger.debug("Processing multi-cardinality primitive list at path: {} (size: {})", rosettaPath.buildPath(), collection.size());

        FieldMap parentTarget = currentTarget(rosettaPath);
        RosettaPath leafPath = toRelativePath(rosettaPath);
        String counterLabel = labelProvider.getLabel(leafPath);
        if (counterLabel == null) {
            addError(RuneFIXSerializerIssue.Code.MISSING_LABEL_MAPPING, rosettaPath, String.format(
                            "No FIX label mapping found for primitive repeating group path '%s'",
                            leafPath == null ? "" : leafPath.buildPath())
            );
            return;
        }

        int counterTag = dictionary.getFieldTag(counterLabel);
        if (counterTag <= 0) {
            addError(RuneFIXSerializerIssue.Code.MISSING_FIX_TAG, rosettaPath, String.format(
                            "No FIX tag found for primitive repeating group label '%s'",
                            counterLabel)
            );
            return;
        }

        DataDictionary.GroupInfo groupInfo = getGroupInfoFor(parentTarget, counterTag);
        if (groupInfo == null) {
            addError(RuneFIXSerializerIssue.Code.MISSING_GROUP_METADATA, rosettaPath, String.format(
                            "No FIX group metadata found for message type '%s' and counter tag '%s'",
                            msgType, counterTag)
            );
            return;
        }

        int delimiterTag = groupInfo.getDelimiterField();
        if (logger.isDebugEnabled())
            logger.debug("Resolved primitive group '{}' -> CounterTag: {}, DelimiterTag: {}", counterLabel, counterTag, delimiterTag);

        for (T item : collection) {
            if (item != null) {
                DataDictionary groupDictionary = groupInfo.getDataDictionary();
                Group group = new Group(counterTag, delimiterTag, groupDictionary.getOrderedFields());
                FIXSerializerFormatter.setField(group, groupDictionary, delimiterTag, item);
                parentTarget.addGroup(group);
                if (logger.isTraceEnabled())
                    logger.trace("Added primitive group item: Tag {} = {}", delimiterTag, item);
            }
        }
    }

    private void addError(RuneFIXSerializerIssue.Code code, RosettaPath path, String message) {
        String pathValue = path == null ? "" : path.buildPath();
        issues.add(new RuneFIXSerializerIssue(RuneFIXSerializerIssue.Severity.ERROR, code, pathValue, message));
        if (logger.isErrorEnabled())
            logger.error("{} [{}]: {}", pathValue, code, message);
    }

    @Override
    public RuneFIXSerializerReport report() {
        return report(false);
    }

    public RuneFIXSerializerReport report(boolean validate) {
        if (logger.isDebugEnabled()) {
            logger.debug("Finalizing FIX Body generation.");
        }

        if (validate) {
            return validateBodyOnlyReport();
        }

        return new RuneFIXSerializerReport(rootMessage, issues);
    }
}
