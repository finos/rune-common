package test.basic.validation.exists;

import test.basic.BasicSingle;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidationResult.ValidationType;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class BasicSingleOnlyExistsValidator implements ValidatorWithArg<BasicSingle, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends BasicSingle> ValidationResult<BasicSingle> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("booleanType", ExistenceChecker.isSet((Boolean) o.getBooleanType()))
				.put("numberType", ExistenceChecker.isSet((BigDecimal) o.getNumberType()))
				.put("parameterisedNumberType", ExistenceChecker.isSet((BigDecimal) o.getParameterisedNumberType()))
				.put("parameterisedStringType", ExistenceChecker.isSet((String) o.getParameterisedStringType()))
				.put("stringType", ExistenceChecker.isSet((String) o.getStringType()))
				.put("timeType", ExistenceChecker.isSet((LocalTime) o.getTimeType()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("BasicSingle", ValidationType.ONLY_EXISTS, "BasicSingle", path, "");
		}
		return failure("BasicSingle", ValidationType.ONLY_EXISTS, "BasicSingle", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
