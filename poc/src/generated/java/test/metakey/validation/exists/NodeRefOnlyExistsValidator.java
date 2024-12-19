package test.metakey.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidationResult.ValidationType;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.metakey.A;
import test.metakey.NodeRef;
import test.metakey.metafields.ReferenceWithMetaA;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class NodeRefOnlyExistsValidator implements ValidatorWithArg<NodeRef, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends NodeRef> ValidationResult<NodeRef> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("typeA", ExistenceChecker.isSet((A) o.getTypeA()))
				.put("aReference", ExistenceChecker.isSet((ReferenceWithMetaA) o.getAReference()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("NodeRef", ValidationType.ONLY_EXISTS, "NodeRef", path, "");
		}
		return failure("NodeRef", ValidationType.ONLY_EXISTS, "NodeRef", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
