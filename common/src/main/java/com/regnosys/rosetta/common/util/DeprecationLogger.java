package com.regnosys.rosetta.common.util;

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

import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Emits a runtime deprecation warning at most once per key, so callers of deprecated APIs get an
 * actionable signal in their logs without the warning being repeated on every call.
 */
public final class DeprecationLogger {

    private static final Set<String> ALREADY_LOGGED = ConcurrentHashMap.newKeySet();

    private DeprecationLogger() {
    }

    /**
     * Logs {@code message} as a warning on {@code logger}, but only the first time {@code key} is seen.
     * Use a key that identifies the deprecated usage (e.g. the method name, optionally plus the caller's
     * class) so distinct usages each warn once while repeated calls stay silent.
     */
    public static void warnOnce(Logger logger, String key, String message, Object... args) {
        if (ALREADY_LOGGED.add(key)) {
            logger.warn(message, args);
        }
    }
}
