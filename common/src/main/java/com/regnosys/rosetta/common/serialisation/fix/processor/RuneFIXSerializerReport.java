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

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.process.Processor;
import quickfix.Message;

import java.util.List;
import java.util.stream.Collectors;

public class RuneFIXSerializerReport implements Processor.Report {

    private final Message fixMessage;
    private final List<RuneFIXSerializerIssue> issues;

    public RuneFIXSerializerReport(Message fixMessage, List<RuneFIXSerializerIssue> issues) {
        this.fixMessage = fixMessage;
        this.issues = ImmutableList.copyOf(issues);
    }

    public Message getFixMessage() {
        return fixMessage;
    }

    public List<RuneFIXSerializerIssue> getIssues() {
        return issues;
    }

    public boolean hasErrors() {
        return issues.stream()
                .anyMatch(issue -> issue.getSeverity() == RuneFIXSerializerIssue.Severity.ERROR);
    }

    public List<RuneFIXSerializerIssue> getErrors() {
        return issues.stream()
                .filter(issue -> issue.getSeverity() == RuneFIXSerializerIssue.Severity.ERROR)
                .collect(Collectors.toList());
    }
}
