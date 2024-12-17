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
import test.metakey.NodeRef;
import test.metakey.validation.NodeRefTypeFormatValidator;
import test.metakey.validation.NodeRefValidator;
import test.metakey.validation.exists.NodeRefOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=NodeRef.class)
public class NodeRefMeta implements RosettaMetaData<NodeRef> {

	@Override
	public List<Validator<? super NodeRef>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super NodeRef, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}

	@Override
	public Validator<? super NodeRef> validator() {
		return new NodeRefValidator();
	}

	@Override
	public Validator<? super NodeRef> typeFormatValidator() {
		return new NodeRefTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super NodeRef, Set<String>> onlyExistsValidator() {
		return new NodeRefOnlyExistsValidator();
	}
}
