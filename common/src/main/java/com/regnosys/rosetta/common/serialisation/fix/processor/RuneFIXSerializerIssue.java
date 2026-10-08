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

import java.util.Objects;

public final class RuneFIXSerializerIssue {

    public enum Code {
        FIX_VALIDATION_FAILED,
        MISSING_LABEL_MAPPING,
        MISSING_FIX_TAG,
        MISSING_GROUP_METADATA
    }

    public enum Severity {
        WARNING,
        ERROR
    }

    private final Severity severity;
    private final Code code;
    private final String path;
    private final String message;

    public RuneFIXSerializerIssue(
            Severity severity,
            Code code,
            String path,
            String message) {
        this.severity = Objects.requireNonNull(severity);
        this.code = code;
        this.path = path;
        this.message = message;
    }

    public Severity getSeverity() {
        return severity;
    }

    public Code getCode() {
        return code;
    }

    public String getPath() {
        return path;
    }

    public String getMessage() {
        return message;
    }
}
