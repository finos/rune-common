package com.regnosys.rosetta.common.validation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rosetta.lib.postprocess.PostProcessorReport;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.lib.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("FieldCanBeLocal") // Used by Jackson
public class ValidationReport implements PostProcessorReport, Processor.Report {

	private static final Logger LOGGER = LoggerFactory.getLogger(ValidationReport.class);

	@JsonProperty
	private final List<ValidationResult<?>> validationResults;
	@JsonIgnore
	private final RosettaModelObject resultObject;

	public ValidationReport(RosettaModelObject resultObject, List<ValidationResult<?>> validationResults) {
		this.resultObject = resultObject;
		validationResults.sort(Comparator.comparing(ValidationResult::isSuccess, Boolean::compare));
		this.validationResults = validationResults;
	}

	@JsonProperty
	public boolean success() {
		return !failure();
	}

	public List<ValidationResult<?>> validationFailures() {
		return validationResults.stream()
				.filter(r -> !r.isSuccess())
				.collect(Collectors.toList());
	}

	public List<ValidationResult<?>> results() {
		return validationResults;
	}

	public void logReport() {
		for (ValidationResult<?> validationResult : validationResults) {
			if (!validationResult.isSuccess()) {
				LOGGER.error(validationResult.toString());
			} else {
				LOGGER.debug(validationResult.toString());
			}
		}
	}

	private boolean failure() {
		return !validationFailures().isEmpty();
	}

	public List<ValidationResult<?>> getValidationResults() {
		return validationResults;
	}

	@Override
	public RosettaModelObject getResultObject() {
		return resultObject;
	}
}
