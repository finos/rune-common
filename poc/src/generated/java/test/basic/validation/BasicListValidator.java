package test.basic.validation;

import test.basic.BasicList;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidationResult.ValidationType;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperators.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class BasicListValidator implements Validator<BasicList> {

	private List<ComparisonResult> getComparisonResults(BasicList o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("booleanTypes", (List<Boolean>) o.getBooleanTypes() == null ? 0 : o.getBooleanTypes().size(), 1, 0), 
				checkCardinality("numberTypes", (List<BigDecimal>) o.getNumberTypes() == null ? 0 : o.getNumberTypes().size(), 1, 0), 
				checkCardinality("parameterisedNumberTypes", (List<BigDecimal>) o.getParameterisedNumberTypes() == null ? 0 : o.getParameterisedNumberTypes().size(), 1, 0), 
				checkCardinality("stringTypes", (List<String>) o.getStringTypes() == null ? 0 : o.getStringTypes().size(), 1, 0), 
				checkCardinality("timeTypes", (List<LocalTime>) o.getTimeTypes() == null ? 0 : o.getTimeTypes().size(), 1, 0)
			);
	}

	@Override
	public ValidationResult<BasicList> validate(RosettaPath path, BasicList o) {
		String error = getComparisonResults(o)
			.stream()
			.filter(res -> !res.get())
			.map(res -> res.getError())
			.collect(joining("; "));

		if (!isNullOrEmpty(error)) {
			return failure("BasicList", ValidationType.CARDINALITY, "BasicList", path, "", error);
		}
		return success("BasicList", ValidationType.CARDINALITY, "BasicList", path, "");
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, BasicList o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("BasicList", ValidationType.CARDINALITY, "BasicList", path, "", res.getError());
				}
				return success("BasicList", ValidationType.CARDINALITY, "BasicList", path, "");
			})
			.collect(toList());
	}

}
