package test.basic.validation;

import test.basic.BasicList;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidationResult.ValidationType;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperators.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class BasicListTypeFormatValidator implements Validator<BasicList> {

	private List<ComparisonResult> getComparisonResults(BasicList o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("parameterisedNumberTypes", o.getParameterisedNumberTypes(), of(18), of(2), empty(), empty())
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
			return failure("BasicList", ValidationType.TYPE_FORMAT, "BasicList", path, "", error);
		}
		return success("BasicList", ValidationType.TYPE_FORMAT, "BasicList", path, "");
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, BasicList o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("BasicList", ValidationType.TYPE_FORMAT, "BasicList", path, "", res.getError());
				}
				return success("BasicList", ValidationType.TYPE_FORMAT, "BasicList", path, "");
			})
			.collect(toList());
	}

}
