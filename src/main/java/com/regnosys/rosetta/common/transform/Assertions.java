package com.regnosys.rosetta.common.transform;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Assertions {

    private final Integer modelValidationFailures;
    private final Boolean schemaValidationFailure;
    private final Boolean runtimeError;

    @JsonCreator
    public Assertions(@JsonProperty("modelValidationFailures") Integer modelValidationFailures,
                      @JsonProperty("schemaValidationFailure") Boolean schemaValidationFailure,
                      @JsonProperty("runtimeError") Boolean runtimeError) {
        this.modelValidationFailures = modelValidationFailures;
        this.schemaValidationFailure = schemaValidationFailure;
        this.runtimeError = runtimeError;
    }

    public Integer getModelValidationFailures() {
        return modelValidationFailures;
    }

    public Boolean isSchemaValidationFailure() {
        return schemaValidationFailure;
    }

    public Boolean isRuntimeError() {
        return runtimeError;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Assertions)) return false;
        Assertions that = (Assertions) o;
        return Objects.equals(getModelValidationFailures(), that.getModelValidationFailures()) && Objects.equals(schemaValidationFailure, that.schemaValidationFailure) && Objects.equals(runtimeError, that.runtimeError);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getModelValidationFailures(), schemaValidationFailure, runtimeError);
    }

    @Override
    public String toString() {
        return "Assertions{" +
                "modelValidationFailures=" + modelValidationFailures +
                ", schemaValidationFailure=" + schemaValidationFailure +
                ", runtimeError=" + runtimeError +
                '}';
    }
}
