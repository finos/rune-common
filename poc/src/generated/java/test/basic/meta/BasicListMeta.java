package test.basic.meta;

import test.basic.BasicList;
import test.basic.validation.BasicListTypeFormatValidator;
import test.basic.validation.BasicListValidator;
import test.basic.validation.exists.BasicListOnlyExistsValidator;
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
@RosettaMeta(model=BasicList.class)
public class BasicListMeta implements RosettaMetaData<BasicList> {

	@Override
	public List<Validator<? super BasicList>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super BasicList, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}

	@Override
	public Validator<? super BasicList> validator() {
		return new BasicListValidator();
	}

	@Override
	public Validator<? super BasicList> typeFormatValidator() {
		return new BasicListTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super BasicList, Set<String>> onlyExistsValidator() {
		return new BasicListOnlyExistsValidator();
	}
}
