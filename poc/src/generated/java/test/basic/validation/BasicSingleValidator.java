package test.basic.validation;

import test.basic.BasicSingle;
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

public class BasicSingleValidator implements Validator<BasicSingle> {

	private List<ComparisonResult> getComparisonResults(BasicSingle o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("booleanType", (Boolean) o.getBooleanType() != null ? 1 : 0, 1, 1), 
				checkCardinality("numberType", (BigDecimal) o.getNumberType() != null ? 1 : 0, 1, 1), 
				checkCardinality("parameterisedNumberType", (BigDecimal) o.getParameterisedNumberType() != null ? 1 : 0, 1, 1), 
				checkCardinality("parameterisedStringType", (String) o.getParameterisedStringType() != null ? 1 : 0, 1, 1), 
				checkCardinality("stringType", (String) o.getStringType() != null ? 1 : 0, 1, 1), 
				checkCardinality("timeType", (LocalTime) o.getTimeType() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public ValidationResult<BasicSingle> validate(RosettaPath path, BasicSingle o) {
		String error = getComparisonResults(o)
			.stream()
			.filter(res -> !res.get())
			.map(res -> res.getError())
			.collect(joining("; "));

		if (!isNullOrEmpty(error)) {
			return failure("BasicSingle", ValidationType.CARDINALITY, "BasicSingle", path, "", error);
		}
		return success("BasicSingle", ValidationType.CARDINALITY, "BasicSingle", path, "");
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, BasicSingle o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("BasicSingle", ValidationType.CARDINALITY, "BasicSingle", path, "", res.getError());
				}
				return success("BasicSingle", ValidationType.CARDINALITY, "BasicSingle", path, "");
			})
			.collect(toList());
	}

}
