package com.regnosys.rosetta.common.validation;

import com.rosetta.model.lib.validation.ValidationResult;

public class ValidationFailure {

    private final ValidationResult.ValidationType validationType;
    private final String ruleName;
    private final String failureReason;
    private final String modelClassName;

    public ValidationFailure(ValidationResult.ValidationType validationType, String ruleName, String failureReason, String modelClassName) {
        this.validationType = validationType;
        this.ruleName = ruleName;
        this.failureReason = failureReason;
        this.modelClassName = modelClassName;
    }

    public ValidationResult.ValidationType getValidationType() {
        return validationType;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getModelClassName() {
        return modelClassName;
    }
}
