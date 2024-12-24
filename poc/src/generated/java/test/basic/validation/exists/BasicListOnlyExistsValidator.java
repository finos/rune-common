package test.basic.validation.exists;

import test.basic.BasicList;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidationResult.ValidationType;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class BasicListOnlyExistsValidator implements ValidatorWithArg<BasicList, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends BasicList> ValidationResult<BasicList> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("booleanTypes", ExistenceChecker.isSet((List<Boolean>) o.getBooleanTypes()))
				.put("numberTypes", ExistenceChecker.isSet((List<BigDecimal>) o.getNumberTypes()))
				.put("parameterisedNumberTypes", ExistenceChecker.isSet((List<BigDecimal>) o.getParameterisedNumberTypes()))
				.put("stringTypes", ExistenceChecker.isSet((List<String>) o.getStringTypes()))
				.put("timeTypes", ExistenceChecker.isSet((List<LocalTime>) o.getTimeTypes()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("BasicList", ValidationType.ONLY_EXISTS, "BasicList", path, "");
		}
		return failure("BasicList", ValidationType.ONLY_EXISTS, "BasicList", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
