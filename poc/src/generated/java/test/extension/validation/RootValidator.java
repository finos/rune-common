package test.extension.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidationResult.ValidationType;
import com.rosetta.model.lib.validation.Validator;
import test.extension.A;
import test.extension.B;
import test.extension.Root;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperators.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class RootValidator implements Validator<Root> {

	private List<ComparisonResult> getComparisonResults(Root o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("typeA", (A) o.getTypeA() != null ? 1 : 0, 0, 1), 
				checkCardinality("typeB", (B) o.getTypeB() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public ValidationResult<Root> validate(RosettaPath path, Root o) {
		String error = getComparisonResults(o)
			.stream()
			.filter(res -> !res.get())
			.map(res -> res.getError())
			.collect(joining("; "));

		if (!isNullOrEmpty(error)) {
			return failure("Root", ValidationType.CARDINALITY, "Root", path, "", error);
		}
		return success("Root", ValidationType.CARDINALITY, "Root", path, "");
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Root o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Root", ValidationType.CARDINALITY, "Root", path, "", res.getError());
				}
				return success("Root", ValidationType.CARDINALITY, "Root", path, "");
			})
			.collect(toList());
	}

}
