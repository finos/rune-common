package com.regnosys.granite.projector;

import com.regnosys.granite.ingestor.parser.InputValidationReport;
import com.regnosys.rosetta.common.util.Report;
import com.regnosys.rosetta.common.validation.ValidationReport;
import com.rosetta.model.lib.RosettaModelObject;

import static com.google.common.base.Preconditions.checkNotNull;

public class ProjectionReport<R extends RosettaModelObject, T> extends Report<R> {

	private final T projectedInstance;
	private final String projectedInstanceAsString;
	private final InputValidationReport inputValidation;
	private final ValidationReport validationReport;

	public ProjectionReport(R rosettaModelInstance, T projectedInstance, String projectedInstanceAsString, InputValidationReport inputValidation, ValidationReport validationReport) {
		super(rosettaModelInstance);
		this.projectedInstance = projectedInstance;
		this.projectedInstanceAsString = projectedInstanceAsString;
		this.inputValidation = checkNotNull(inputValidation);
		this.validationReport = validationReport;
	}

	public boolean isSuccess() {
		return inputValidation.getErrors().isEmpty() && projectedInstance != null && projectedInstanceAsString != null;
	}

	public T getProjectedInstance() {
		return projectedInstance;
	}

	public String getProjectedInstanceAsString() {
		return projectedInstanceAsString;
	}

	public InputValidationReport getInputValidation() {
		return inputValidation;
	}

	public ValidationReport getValidationReport() {
		return validationReport;
	}
}
