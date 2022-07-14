package com.regnosys.rosetta.common.postprocess.qualify;

import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.qualify.QualifyResult;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class QualificationResult {

    private final Optional<RosettaPath> path;
    private final Class<?> qualifiedRosettaObjectType;
    private final Optional<QualifyResult> uniqueSuccessQualifyResult;
    private final List<QualifyResult> allQualifyResults;

    public QualificationResult(Optional<RosettaPath> path,
                               Class<?> qualifiedRosettaObjectType,
                               Optional<QualifyResult> uniqueSuccessQualifyResult,
                               List<QualifyResult> allQualifyResults) {
        this.path = path;
        this.qualifiedRosettaObjectType = qualifiedRosettaObjectType;
        this.uniqueSuccessQualifyResult = uniqueSuccessQualifyResult;
        this.allQualifyResults = allQualifyResults;
    }

    public Optional<RosettaPath> getPath() {
        return path;
    }

    public String getBuildPath() {
        return path.map(p -> p.buildPath()).orElse("");
    }

    public Class<?> getQualifiedRosettaObjectType() {
        return qualifiedRosettaObjectType;
    }

    /**
     * @return unique successful qualify result if present, otherwise (if unmatched or multiple matches) returns empty.
     */
    public Optional<QualifyResult> getUniqueSuccessQualifyResult() {
        return uniqueSuccessQualifyResult;
    }

    public boolean isSuccess() {
        return uniqueSuccessQualifyResult.isPresent();
    }

    /**
     * @return qualify results from all executed logic
     */
    public List<QualifyResult> getAllQualifyResults() {
        return allQualifyResults;
    }

    @Override
    public String toString() {
        if(uniqueSuccessQualifyResult.isPresent()) {
            return String.format("QualificationResult { SUCCESS on [%s:%s] }",
                    qualifiedRosettaObjectType.getSimpleName(),
                    uniqueSuccessQualifyResult.get().getName());
        }
        else {
            // Log multiple matches (if there are any)
            List<String> successResults = allQualifyResults.stream()
                    .filter(QualifyResult::isSuccess)
                    .map(QualifyResult::getName)
                    .collect(Collectors.toList());
            List<String> errors = allQualifyResults.stream()
                    .filter(r -> !r.isSuccess())
                    .map(this::getQualifyFunctionErrors)
                    .collect(Collectors.toList());
            return String.format("QualificationResult { FAILURE on [%s] because [%s]%s }",
                    qualifiedRosettaObjectType.getSimpleName(),
                    successResults.isEmpty() ?
                            "UNMATCHED" :
                            "MULTIPLE_MATCHES: " + String.join(",", successResults),
                    successResults.isEmpty() ? String.format(", errors: [%s]", errors) : "");
        }
    }

    private String getQualifyFunctionErrors(QualifyResult qualifyFunctionResult) {
        List<String> failedFunctionErrors = qualifyFunctionResult.getExpressionDataRuleResults().stream()
                .filter(e -> !e.isSuccess())
                .map(QualifyResult.ExpressionDataRuleResult::getError)
                .collect(Collectors.toList());
        return String.format("%s%s" , qualifyFunctionResult.getName(), failedFunctionErrors);
    }
}
