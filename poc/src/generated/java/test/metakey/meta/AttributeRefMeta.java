package test.metakey.meta;

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
import test.metakey.AttributeRef;
import test.metakey.validation.AttributeRefTypeFormatValidator;
import test.metakey.validation.AttributeRefValidator;
import test.metakey.validation.exists.AttributeRefOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=AttributeRef.class)
public class AttributeRefMeta implements RosettaMetaData<AttributeRef> {

	@Override
	public List<Validator<? super AttributeRef>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super AttributeRef, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}

	@Override
	public Validator<? super AttributeRef> validator() {
		return new AttributeRefValidator();
	}

	@Override
	public Validator<? super AttributeRef> typeFormatValidator() {
		return new AttributeRefTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super AttributeRef, Set<String>> onlyExistsValidator() {
		return new AttributeRefOnlyExistsValidator();
	}
}
