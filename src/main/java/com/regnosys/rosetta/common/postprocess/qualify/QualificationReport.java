package com.regnosys.rosetta.common.postprocess.qualify;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rosetta.lib.postprocess.PostProcessorReport;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

public class QualificationReport implements PostProcessorReport {

    public static final QualificationReport SUCCESS = new QualificationReport(null, Collections.emptyList());

    private static final Logger LOGGER = LoggerFactory.getLogger(QualificationReport.class);

    private final RosettaModelObject ingestedObject;
    private final Collection<QualificationResult> results;
    private final int qualifiableObjectsCount;
    private final int uniquelyQualifiedObjectsCount;

    public QualificationReport(RosettaModelObject ingestedObject, Collection<QualificationResult> results) {
        this.ingestedObject = ingestedObject;
        this.results = results;
        this.qualifiableObjectsCount = results.size();
        this.uniquelyQualifiedObjectsCount = (int) results.stream()
                .map(QualificationResult::getUniqueSuccessQualifyResult)
                .filter(Optional::isPresent)
                .count();
    }

    public RosettaModelObject getIngestedObject() { 
        return ingestedObject; 
    }

    public Collection<QualificationResult> getResults() {
        return results;
    }

    public int getQualifiableObjectsCount() {
        return qualifiableObjectsCount;
    }

    public int getUniquelyQualifiedObjectsCount() {
        return uniquelyQualifiedObjectsCount;
    }

    public void logReport() {
        LOGGER.info("QualificationReport {} [ qualifiable objects found {}, uniquely qualified objects {}, results: {} ]",
                qualifiableObjectsCount == 0 ? "NO_RESULT" : qualifiableObjectsCount == uniquelyQualifiedObjectsCount ? "SUCCESS" : "FAILURE",
                qualifiableObjectsCount,
                uniquelyQualifiedObjectsCount,
                results.stream().map(QualificationResult::toString).collect(Collectors.toList()));
    }

    @JsonIgnore
	@Override
	public RosettaModelObjectBuilder getResultObject() {
		return ingestedObject.toBuilder();
	}
}
