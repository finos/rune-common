package com.regnosys.rosetta.common.serialisation.csv;


import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;

import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class UserTypeFormatValidator implements Validator<User> {

    private List<ComparisonResult> getComparisonResults(User o) {
        return Lists.<ComparisonResult>newArrayList(
        );
    }

    @Override
    public List<ValidationResult<?>> getValidationResults(RosettaPath path, User o) {
        return getComparisonResults(o)
                .stream()
                .map(res -> {
                    if (!isNullOrEmpty(res.getError())) {
                        return failure("User", ValidationResult.ValidationType.TYPE_FORMAT, "User", path, "", res.getError());
                    }
                    return success("User", ValidationResult.ValidationType.TYPE_FORMAT, "User", path, "");
                })
                .collect(toList());
    }

}
