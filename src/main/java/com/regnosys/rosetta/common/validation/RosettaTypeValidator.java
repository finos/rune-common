package com.regnosys.rosetta.common.validation;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.regnosys.rosetta.common.hashing.SimpleBuilderProcessor;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.PostProcessStep;
import com.rosetta.model.lib.validation.ValidationResult;

public class RosettaTypeValidator extends SimpleBuilderProcessor implements PostProcessStep{

	private static final Logger LOGGER = LoggerFactory.getLogger(RosettaTypeValidator.class);

	private ValidationReport result;

	@Override
	public <T extends RosettaModelObject> ValidationReport runProcessStep(Class<T> topClass,
			RosettaModelObjectBuilder builder) {
		LOGGER.debug("Running validation for " + topClass.getSimpleName());
		builder.prune();
		ValidationReport report = new ValidationReport(builder, new ArrayList<>());
		result = report;
		RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
		this.processRosetta(path, topClass, builder, null);
		builder.process(path, this);
		return report;
	}

	@Override
	public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType,
			RosettaModelObjectBuilder builder, RosettaModelObjectBuilder parent,
			AttributeMeta... metas) {
		if (builder==null) return false;
		RosettaMetaData<? extends RosettaModelObject> metaData = builder.metaData();
		List<ValidationResult<?>> validationResults = result.getValidationResults();
		metaData.dataRules().forEach(dr->validationResults.add(dr.validate(path, builder)));
		metaData.choiceRuleValidators().forEach(dr->validationResults.add(dr.validate(path, builder)));
		if (metaData.validator()!=null) validationResults.add(metaData.validator().validate(path, builder));
		return true;
	}

	@Override
	public Report report() {
		return result;
	}

	@Override
	public String getName() {
		return "Rosetta type validator PostProcressor";
	}

	@Override
	public <T> void processBasic(RosettaPath path, Class<T> rosettaType, T instance, RosettaModelObjectBuilder parent,
			AttributeMeta... metas) {
		//basic types don't get validated
	}

	@Override
	public Integer getPriority() {
		return 100;
	}
}
