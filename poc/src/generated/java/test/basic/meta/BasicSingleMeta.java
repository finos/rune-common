package test.basic.meta;

import test.basic.BasicSingle;
import test.basic.validation.BasicSingleTypeFormatValidator;
import test.basic.validation.BasicSingleValidator;
import test.basic.validation.exists.BasicSingleOnlyExistsValidator;
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


/**
 * @version 0.0.0
 */
@RosettaMeta(model=BasicSingle.class)
public class BasicSingleMeta implements RosettaMetaData<BasicSingle> {

	@Override
	public List<Validator<? super BasicSingle>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super BasicSingle, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}

	@Override
	public Validator<? super BasicSingle> validator() {
		return new BasicSingleValidator();
	}

	@Override
	public Validator<? super BasicSingle> typeFormatValidator() {
		return new BasicSingleTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super BasicSingle, Set<String>> onlyExistsValidator() {
		return new BasicSingleOnlyExistsValidator();
	}
}
