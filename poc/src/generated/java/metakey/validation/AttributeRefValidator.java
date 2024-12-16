package metakey.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidationResult.ValidationType;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.metafields.FieldWithMetaDate;
import com.rosetta.model.metafields.ReferenceWithMetaDate;
import java.util.List;
import metakey.AttributeRef;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperators.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class AttributeRefValidator implements Validator<AttributeRef> {

	private List<ComparisonResult> getComparisonResults(AttributeRef o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("dateField", (FieldWithMetaDate) o.getDateField() != null ? 1 : 0, 0, 1), 
				checkCardinality("dateReference", (ReferenceWithMetaDate) o.getDateReference() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public ValidationResult<AttributeRef> validate(RosettaPath path, AttributeRef o) {
		String error = getComparisonResults(o)
			.stream()
			.filter(res -> !res.get())
			.map(res -> res.getError())
			.collect(joining("; "));

		if (!isNullOrEmpty(error)) {
			return failure("AttributeRef", ValidationType.CARDINALITY, "AttributeRef", path, "", error);
		}
		return success("AttributeRef", ValidationType.CARDINALITY, "AttributeRef", path, "");
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, AttributeRef o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("AttributeRef", ValidationType.CARDINALITY, "AttributeRef", path, "", res.getError());
				}
				return success("AttributeRef", ValidationType.CARDINALITY, "AttributeRef", path, "");
			})
			.collect(toList());
	}

}
