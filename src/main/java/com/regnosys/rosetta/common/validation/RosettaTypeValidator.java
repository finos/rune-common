package com.regnosys.rosetta.common.validation;

import com.google.inject.Inject;
import com.regnosys.rosetta.common.hashing.SimpleBuilderProcessor;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.PostProcessStep;
import com.rosetta.model.lib.validation.ModelObjectValidator;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RosettaTypeValidator implements PostProcessStep, ModelObjectValidator {

	private static final Logger LOGGER = LoggerFactory.getLogger(RosettaTypeValidator.class);

	@Inject
	private ValidatorFactory validatorFactory;

	@Override
	public <T extends RosettaModelObject> ValidationReport runProcessStep(Class<T> topClass, RosettaModelObjectBuilder builder) {
		LOGGER.debug("Running validation for " + topClass.getSimpleName());
		builder.prune();
		ValidationReport report = new ValidationReport(builder, new ArrayList<>());
		RosettaTypeProcessor processor = new RosettaTypeProcessor(report);
		RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
		processor.processRosetta(path, topClass, builder, null);
		builder.process(path, processor);
		return report;
	}

	class RosettaTypeProcessor extends SimpleBuilderProcessor {
		private ValidationReport result;
		public RosettaTypeProcessor(ValidationReport report) {
			result = report;
		}

		@Override
		public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<R> rosettaType,
				RosettaModelObjectBuilder builder, RosettaModelObjectBuilder parent,
				AttributeMeta... metas) {
			if (builder==null) return false;
			RosettaMetaData<? extends RosettaModelObject> metaData = builder.metaData();
			List<ValidationResult<?>> validationResults = result.getValidationResults();
			metaData.dataRules(validatorFactory).forEach(dr->validationResults.add(dr.validate(path, builder)));
			metaData.choiceRuleValidators().forEach(dr->validationResults.add(dr.validate(path, builder)));
			if (metaData.validator()!=null) validationResults.add(metaData.validator().validate(path, builder));
			return true;
		}

		@Override
		public Report report() {
			return result;
		}
	}

	@Override
	public String getName() {
		return "Rosetta type validator PostProcessor";
	}

	@Override
	public Integer getPriority() {
		return 100;
	}


	/**
	 * Runs the process step and collects errors. Throws an exception if validation fails
	 *
	 * @param <T>
	 * @param topClass
	 * @param modelObject
	 * @throws RuntimeException if validation fails
	 */
	@Override
	public <T extends RosettaModelObject> void validateAndFailOnErorr(Class<T> topClass, T modelObject) {
		final StringBuilder errors = new StringBuilder();
		validateAndCollectErrors(topClass, modelObject, (res) -> errors.append(System.lineSeparator()).append(res.toString()));
		if(errors.length() > 0) {
			LOGGER.error("Validation failed for type {}: {}", topClass, errors);
			throw new ModelObjectValidator.ModelObjectValidationException("Object validation failed:" + errors.toString());
		}
	}

	/**
	 * Runs the process step and collects errors. Throws an exception if validation fails
	 *
	 * @param <T>
	 * @param topClass
	 * @param modelObjects
	 * @throws RuntimeException if validation fails
	 */
	@Override
	public <T extends RosettaModelObject> void validateAndFailOnErorr(Class<T> topClass, List<T> modelObjects) {
		final StringBuilder errors = new StringBuilder();
		for (T modelObject : modelObjects) {
			validateAndCollectErrors(topClass, modelObject, (res) -> errors.append(System.lineSeparator()).append(res.toString()));
		}
		if(errors.length() > 0) {
			throw new ModelObjectValidator.ModelObjectValidationException("Object validation failed:" + errors.toString());
		}
	}

	private <T extends RosettaModelObject> void validateAndCollectErrors(Class<T> topClass, T modelObject, Consumer<? super ValidationResult<?>> collector) {
		runProcessStep(topClass, modelObject.toBuilder())
			.getValidationResults()
			.stream().filter((res)-> { return !res.isSuccess();})
			.forEach(collector);
	}
}
