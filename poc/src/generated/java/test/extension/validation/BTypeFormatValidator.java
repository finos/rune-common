package test.extension.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidationResult.ValidationType;
import com.rosetta.model.lib.validation.Validator;
import test.extension.B;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class BTypeFormatValidator implements Validator<B> {

	private List<ComparisonResult> getComparisonResults(B o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public ValidationResult<B> validate(RosettaPath path, B o) {
		String error = getComparisonResults(o)
			.stream()
			.filter(res -> !res.get())
			.map(res -> res.getError())
			.collect(joining("; "));

		if (!isNullOrEmpty(error)) {
			return failure("B", ValidationType.TYPE_FORMAT, "B", path, "", error);
		}
		return success("B", ValidationType.TYPE_FORMAT, "B", path, "");
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, B o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("B", ValidationType.TYPE_FORMAT, "B", path, "", res.getError());
				}
				return success("B", ValidationType.TYPE_FORMAT, "B", path, "");
			})
			.collect(toList());
	}

}
