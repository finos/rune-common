package com.regnosys.rosetta.common.serialisation;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2025 REGnosys
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

import java.time.ZoneOffset;
import java.time.zone.ZoneRules;
import java.time.zone.ZoneRulesException;
import java.time.zone.ZoneRulesProvider;
import java.util.*;

public class UnknownZoneProvider extends ZoneRulesProvider {
    private static final String UNKNOWN_ZONE_ID = "Unknown";
    private static final ZoneRules UNKNOWN_ZONE_RULES = ZoneRules.of(ZoneOffset.UTC);

    private static final Set<String> ZONE_IDS = Collections.singleton(UNKNOWN_ZONE_ID);

    @Override
    protected Set<String> provideZoneIds() {
        return ZONE_IDS;
    }

    @Override
    protected ZoneRules provideRules(String zoneId, boolean forCaching) {
        if (forCaching) {
            return null;
        }
        if (UNKNOWN_ZONE_ID.equals(zoneId)) {
            return UNKNOWN_ZONE_RULES;
        }
        throw new ZoneRulesException("Unknown zone ID: " + zoneId);
    }

    @Override
    protected NavigableMap<String, ZoneRules> provideVersions(String zoneId) {
        if (UNKNOWN_ZONE_ID.equals(zoneId)) {
            NavigableMap<String, ZoneRules> versionMap = new TreeMap<>();
            versionMap.put("1", UNKNOWN_ZONE_RULES);
            return versionMap;
        }
        throw new ZoneRulesException("Unknown zone ID: " + zoneId);
    }
}
