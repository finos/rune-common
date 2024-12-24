package test.basic.validation;

import test.basic.BasicSingle;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidationResult.ValidationType;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperators.checkNumber;
import static com.rosetta.model.lib.expression.ExpressionOperators.checkString;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class BasicSingleTypeFormatValidator implements Validator<BasicSingle> {

	private List<ComparisonResult> getComparisonResults(BasicSingle o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("parameterisedNumberType", o.getParameterisedNumberType(), of(18), of(2), empty(), empty()), 
				checkString("parameterisedStringType", o.getParameterisedStringType(), 1, of(20), of(Pattern.compile("[a-zA-Z]")))
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
			return failure("BasicSingle", ValidationType.TYPE_FORMAT, "BasicSingle", path, "", error);
		}
		return success("BasicSingle", ValidationType.TYPE_FORMAT, "BasicSingle", path, "");
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, BasicSingle o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("BasicSingle", ValidationType.TYPE_FORMAT, "BasicSingle", path, "", res.getError());
				}
				return success("BasicSingle", ValidationType.TYPE_FORMAT, "BasicSingle", path, "");
			})
			.collect(toList());
	}

}
