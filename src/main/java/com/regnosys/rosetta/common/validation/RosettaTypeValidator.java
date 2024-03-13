package com.regnosys.rosetta.common.validation;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.inject.Inject;
import com.regnosys.rosetta.common.util.SimpleProcessor;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.PostProcessStep;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class RosettaTypeValidator implements PostProcessStep {

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
            Streams.<Validator<? super R>>concat(
                    metaData.dataRules(validatorFactory).stream(),
                    Optional.ofNullable(metaData.validator()).map(Stream::of).orElse(Stream.empty()),
                    Optional.ofNullable(metaData.typeFormatValidator()).map(Stream::of).orElse(Stream.empty())
                ).forEach(validator -> validationResults.addAll(validator.getValidationResults(path, instance)));
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
}
