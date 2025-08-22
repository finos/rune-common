package com.regnosys.rosetta.common.serialisation.csv;


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
