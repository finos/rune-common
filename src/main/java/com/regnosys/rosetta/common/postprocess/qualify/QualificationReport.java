package com.regnosys.rosetta.common.postprocess.qualify;

/*-
 * ==============
 * Rosetta Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
 * ==============
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============
 */


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
