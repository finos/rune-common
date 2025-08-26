package com.regnosys.rosetta.common.serialisation.csv;

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


import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class UserOnlyExistsValidator implements ValidatorWithArg<User, Set<String>> {

    /* Casting is required to ensure types are output to ensure recompilation in Rosetta */
    @Override
    public <T2 extends User> ValidationResult<User> validate(RosettaPath path, T2 o, Set<String> fields) {
        Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
                .put("username", ExistenceChecker.isSet((String) o.getUsername()))
                .put("identifier", ExistenceChecker.isSet((String) o.getIdentifier()))
                .put("firstName", ExistenceChecker.isSet((String) o.getFirstName()))
                .put("lastName", ExistenceChecker.isSet((String) o.getLastName()))
                .build();

        // Find the fields that are set
        Set<String> setFields = fieldExistenceMap.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (setFields.equals(fields)) {
            return success("User", ValidationResult.ValidationType.ONLY_EXISTS, "User", path, "");
        }
        return failure("User", ValidationResult.ValidationType.ONLY_EXISTS, "User", path, "",
                String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
    }
}
