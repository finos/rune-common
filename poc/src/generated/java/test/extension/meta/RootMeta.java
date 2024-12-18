package test.extension.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import test.extension.Root;
import test.extension.validation.RootTypeFormatValidator;
import test.extension.validation.RootValidator;
import test.extension.validation.exists.RootOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Root.class)
public class RootMeta implements RosettaMetaData<Root> {

	@Override
	public List<Validator<? super Root>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Root, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}

	@Override
	public Validator<? super Root> validator() {
		return new RootValidator();
	}

	@Override
	public Validator<? super Root> typeFormatValidator() {
		return new RootTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Root, Set<String>> onlyExistsValidator() {
		return new RootOnlyExistsValidator();
	}
}