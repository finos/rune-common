package com.regnosys.rosetta.common.reports;

/*-
 * #%L
 * Rosetta Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
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
 * #L%
 */

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

@Deprecated
public class RegReportIdentifier {

	private final String body;
	private final List<String> corpusList;
	private final String name;
	private final String generatedJavaClassName;

	@JsonCreator
	public RegReportIdentifier(@JsonProperty("body") String body,
							   @JsonProperty("corpusList") List<String> corpusList,
							   @JsonProperty("name") String name,
							   @JsonProperty("generatedJavaClassName") String generatedJavaClassName) {
		this.body = body;
		this.corpusList = corpusList;
		this.name = name;
		this.generatedJavaClassName = generatedJavaClassName;
	}

	public String getBody() {
		return body;
	}

	public List<String> getCorpusList() {
		return corpusList;
	}

	public String getName() {
		return name;
	}

	public String getGeneratedJavaClassName() {
		return generatedJavaClassName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RegReportIdentifier that = (RegReportIdentifier) o;
		return Objects.equals(getBody(), that.getBody()) && Objects.equals(getCorpusList(), that.getCorpusList()) && Objects.equals(getName(), that.getName()) && Objects.equals(getGeneratedJavaClassName(), that.getGeneratedJavaClassName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getBody(), getCorpusList(), getName(), getGeneratedJavaClassName());
	}
}
