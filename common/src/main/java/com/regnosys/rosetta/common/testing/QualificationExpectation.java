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

import java.util.List;
import java.util.Objects;

public class QualificationExpectation {

	private boolean success;
	private List<Result> qualifyResults;
	private int qualifiableObjectCount;

	public QualificationExpectation(boolean success, List<Result> qualifyResults, int qualifiableObjectCount) {
		this.success = success;
		this.qualifyResults = qualifyResults;
		this.qualifiableObjectCount = qualifiableObjectCount;
	}

	private QualificationExpectation() {
	}

	public boolean isSuccess() {
		return success;
	}

	public int getQualifiableObjectCount() {
		return qualifiableObjectCount;
	}

	public List<Result> getQualifyResults() {
		return qualifyResults;
	}

	protected void setQualifyResults(List<Result> qualifyResults) {
		this.qualifyResults = qualifyResults;
	}

	protected void setSuccess(boolean success) {
		this.success = success;
	}

	protected void setQualifiableObjectCount(int qualifiableObjectCount) {
		this.qualifiableObjectCount = qualifiableObjectCount;
	}

	public static class Result implements Comparable<Result> {

		private String qualifiedName;
		private String qualifiedObjectClass;

		public Result(String qualifiedName, String qualifiedObjectClass) {
			this.qualifiedName = qualifiedName;
			this.qualifiedObjectClass = qualifiedObjectClass;
		}

		protected Result() {}

		public String getQualifiedName() {
			return qualifiedName;
		}

		public String getQualifiedObjectClass() {
			return qualifiedObjectClass;
		}

		protected void setQualifiedName(String qualifiedName) {
			this.qualifiedName = qualifiedName;
		}

		protected void setQualifiedObjectClass(String qualifiedObjectClass) {
			this.qualifiedObjectClass = qualifiedObjectClass;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			Result result = (Result) o;
			return Objects.equals(qualifiedName, result.qualifiedName)
					&& Objects.equals(qualifiedObjectClass, result.qualifiedObjectClass);
		}

		@Override
		public int hashCode() {
			return Objects.hash(qualifiedName, qualifiedObjectClass);
		}

		@Override
		public String toString() {
			return "Result{" + "qualifiedName='" + qualifiedName + '\'' + ", qualifiedObjectClass="
					+ qualifiedObjectClass + '}';
		}

		@Override
		public int compareTo(Result o) {
			return this.qualifiedName.compareTo(o.qualifiedName);
		}
	}
}
