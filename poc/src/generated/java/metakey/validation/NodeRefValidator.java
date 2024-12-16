package metakey.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidationResult.ValidationType;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import metakey.A;
import metakey.NodeRef;
import metakey.metafields.ReferenceWithMetaA;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperators.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class NodeRefValidator implements Validator<NodeRef> {

	private List<ComparisonResult> getComparisonResults(NodeRef o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("typeA", (A) o.getTypeA() != null ? 1 : 0, 0, 1), 
				checkCardinality("aReference", (ReferenceWithMetaA) o.getAReference() != null ? 1 : 0, 0, 1)
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
			return failure("NodeRef", ValidationType.CARDINALITY, "NodeRef", path, "", error);
		}
		return success("NodeRef", ValidationType.CARDINALITY, "NodeRef", path, "");
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, NodeRef o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("NodeRef", ValidationType.CARDINALITY, "NodeRef", path, "", res.getError());
				}
				return success("NodeRef", ValidationType.CARDINALITY, "NodeRef", path, "");
			})
			.collect(toList());
	}

}
