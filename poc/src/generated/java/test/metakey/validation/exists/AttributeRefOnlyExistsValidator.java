package test.metakey.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidationResult.ValidationType;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import com.rosetta.model.metafields.FieldWithMetaDate;
import com.rosetta.model.metafields.ReferenceWithMetaDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.metakey.AttributeRef;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class AttributeRefOnlyExistsValidator implements ValidatorWithArg<AttributeRef, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends AttributeRef> ValidationResult<AttributeRef> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("dateField", ExistenceChecker.isSet((FieldWithMetaDate) o.getDateField()))
				.put("dateReference", ExistenceChecker.isSet((ReferenceWithMetaDate) o.getDateReference()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("AttributeRef", ValidationType.ONLY_EXISTS, "AttributeRef", path, "");
		}
		return failure("AttributeRef", ValidationType.ONLY_EXISTS, "AttributeRef", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
