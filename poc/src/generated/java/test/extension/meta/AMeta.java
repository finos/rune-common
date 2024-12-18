package test.extension.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import test.extension.A;
import test.extension.validation.ATypeFormatValidator;
import test.extension.validation.AValidator;
import test.extension.validation.exists.AOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=A.class)
public class AMeta implements RosettaMetaData<A> {

	@Override
	public List<Validator<? super A>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super A, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}

	@Override
	public Validator<? super A> validator() {
		return new AValidator();
	}

	@Override
	public Validator<? super A> typeFormatValidator() {
		return new ATypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super A, Set<String>> onlyExistsValidator() {
		return new AOnlyExistsValidator();
	}
}