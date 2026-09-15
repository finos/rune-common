package com.regnosys.rosetta.common.testing;

/*-
 * ==============
 * Rune Common
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

import java.util.Comparator;

public class Expectation implements Comparable<Expectation> {
	private String category;
	private String fileName;
	private int excludedPaths;
	private int externalPaths;
	private int outstandingMappings;
	private int validationFailures;
	private QualificationExpectation qualificationExpectation;

	private Expectation(String category, String fileName, int excludedPaths, int externalPaths, int outstandingMappings, int validationFailures, QualificationExpectation qualificationExpectation) {
		this.category = category;
		this.fileName = fileName;
		this.excludedPaths = excludedPaths;
		this.externalPaths = externalPaths;
		this.outstandingMappings = outstandingMappings;
		this.validationFailures = validationFailures;
		this.qualificationExpectation = qualificationExpectation;
	}

	private Expectation() {
	}

	public static Expectation expect(String category, String fileName, int externalPaths, boolean completeMapping, int validationFailures, QualificationExpectation qualificationExpectation) {
		return new Expectation(category, fileName, -1, externalPaths, completeMapping ? 0 : -1, validationFailures, qualificationExpectation);
	}

	public QualificationExpectation getQualificationExpectation() {
		return qualificationExpectation;
	}

	public String getFileName() {
		return fileName;
	}

	protected void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public int getExternalPaths() {
		return externalPaths;
	}

	public void setExternalPaths(int externalPaths) {
		this.externalPaths = externalPaths;
	}

	public int getOutstandingMappings() {
		return outstandingMappings;
	}

	protected void setOutstandingMappings(int outstandingMappings) {
		this.outstandingMappings = outstandingMappings;
	}

	protected void setQualificationExpectation(QualificationExpectation qualificationExpectation) {
		this.qualificationExpectation = qualificationExpectation;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public int getExcludedPaths() {
		return excludedPaths;
	}

	public void setExcludedPaths(int excludedPaths) {
		this.excludedPaths = excludedPaths;
	}

	public int getValidationFailures() {
		return validationFailures;
	}

	public void setValidationFailures(int validationFailures) {
		this.validationFailures = validationFailures;
	}

	@Override
	public int compareTo(Expectation o) {
		return Comparator.<Expectation, String>comparing(Expectation::getFileName).compare(this, o);
	}
}
