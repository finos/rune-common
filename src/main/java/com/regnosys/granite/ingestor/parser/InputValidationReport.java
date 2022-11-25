package com.regnosys.granite.ingestor.parser;

import java.util.Collections;
import java.util.List;

public class InputValidationReport {

    public final static InputValidationReport SUCCESS = new InputValidationReport(Collections.emptyList());

    private final List<String> errors;

    public InputValidationReport(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }

}
