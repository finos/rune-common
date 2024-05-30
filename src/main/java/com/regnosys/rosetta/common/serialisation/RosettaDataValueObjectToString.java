package com.regnosys.rosetta.common.serialisation;

/*-
 * #%L
 * Rune Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
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
 * #L%
 */

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class RosettaDataValueObjectToString {

    public static String toValueString(Object object) {
        if (object instanceof ZonedDateTime) {
            ZonedDateTime zonedDateTime = (ZonedDateTime) object;
            return zonedDateTime.truncatedTo(ChronoUnit.SECONDS)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        return object.toString();
    }
}
