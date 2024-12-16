package metakey.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidationResult.ValidationType;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import metakey.NodeRef;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class NodeRefTypeFormatValidator implements Validator<NodeRef> {

	private List<ComparisonResult> getComparisonResults(NodeRef o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public ValidationResult<NodeRef> validate(RosettaPath path, NodeRef o) {
		String error = getComparisonResults(o)
			.stream()
			.filter(res -> !res.get())
			.map(res -> res.getError())
			.collect(joining("; "));

		if (!isNullOrEmpty(error)) {
			return failure("NodeRef", ValidationType.TYPE_FORMAT, "NodeRef", path, "", error);
		}
		return success("NodeRef", ValidationType.TYPE_FORMAT, "NodeRef", path, "");
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, NodeRef o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("NodeRef", ValidationType.TYPE_FORMAT, "NodeRef", path, "", res.getError());
				}
				return success("NodeRef", ValidationType.TYPE_FORMAT, "NodeRef", path, "");
			})
			.collect(toList());
	}

}
