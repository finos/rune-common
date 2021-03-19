package com.regnosys.rosetta.common.validation;

import com.google.inject.Inject;
import com.regnosys.rosetta.common.util.SimpleProcessor;
import com.rosetta.model.lib.RosettaModelObject;
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
	public <T extends RosettaModelObject> ValidationReport runProcessStep(Class<? extends T> topClass, T instance) {
		LOGGER.debug("Running validation for " + topClass.getSimpleName());
		ValidationReport report = new ValidationReport(instance, new ArrayList<>());
		RosettaTypeProcessor processor = new RosettaTypeProcessor(report);
		RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
		processor.processRosetta(path, topClass, instance, null);
		instance.process(path, processor);
		return report;
	}

	class RosettaTypeProcessor extends SimpleProcessor {
		private ValidationReport result;
		public RosettaTypeProcessor(ValidationReport report) {
			result = report;
		}

		@Override
		public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType,
				R instance, RosettaModelObject parent,
				AttributeMeta... metas) {
			if (instance==null) return false;
			@SuppressWarnings("unchecked")
			RosettaMetaData<R> metaData = (RosettaMetaData<R>)instance.metaData();
			List<ValidationResult<?>> validationResults = result.getValidationResults();
			metaData.dataRules(validatorFactory).forEach(dr->validationResults.add(dr.validate(path, instance)));
			metaData.choiceRuleValidators().forEach(dr->validationResults.add(dr.validate(path, instance)));
			if (metaData.validator()!=null) validationResults.add(metaData.validator().validate(path, instance));
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
	 * Runs the process step and collects errors.
	 *
	 * @param <T>
	 * @param topClass
	 * @param modelObject
	 */
	@Override
	public <T extends RosettaModelObject> void validateAndFailOnErorr(Class<T> topClass, T modelObject) {
		final StringBuilder errors = new StringBuilder();
		validateAndCollectErrors(topClass, modelObject, (res) -> errors.append(System.lineSeparator()).append(res.toString()));
	}

	/**
	 * Runs the process step and collects errors.
	 *
	 * @param <T>
	 * @param topClass
	 * @param modelObjects
	 */
	@Override
	public <T extends RosettaModelObject> void validateAndFailOnErorr(Class<T> topClass, List<? extends T> modelObjects) {
		final StringBuilder errors = new StringBuilder();
		for (T modelObject : modelObjects) {
			validateAndCollectErrors(topClass, modelObject, (res) -> errors.append(System.lineSeparator()).append(res.toString()));
		}
	}

	private <T extends RosettaModelObject> void validateAndCollectErrors(Class<T> topClass, T modelObject, Consumer<? super ValidationResult<?>> collector) {
		runProcessStep(topClass, modelObject)
			.getValidationResults()
			.stream().filter((res)-> { return !res.isSuccess();})
			.forEach(collector);
	}
}
