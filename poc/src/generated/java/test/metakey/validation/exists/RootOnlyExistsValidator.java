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
import test.metakey.AttributeRef;
import test.metakey.NodeRef;
import test.metakey.Root;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class RootOnlyExistsValidator implements ValidatorWithArg<Root, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Root> ValidationResult<Root> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("nodeRef", ExistenceChecker.isSet((NodeRef) o.getNodeRef()))
				.put("attributeRef", ExistenceChecker.isSet((AttributeRef) o.getAttributeRef()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Root", ValidationType.ONLY_EXISTS, "Root", path, "");
		}
		return failure("Root", ValidationType.ONLY_EXISTS, "Root", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}