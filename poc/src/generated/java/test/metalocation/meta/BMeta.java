package test.metalocation.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import test.metalocation.B;
import test.metalocation.validation.BTypeFormatValidator;
import test.metalocation.validation.BValidator;
import test.metalocation.validation.exists.BOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=B.class)
public class BMeta implements RosettaMetaData<B> {

	@Override
	public List<Validator<? super B>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super B, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}

	@Override
	public Validator<? super B> validator() {
		return new BValidator();
	}

	@Override
	public Validator<? super B> typeFormatValidator() {
		return new BTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super B, Set<String>> onlyExistsValidator() {
		return new BOnlyExistsValidator();
	}
}
