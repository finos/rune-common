package com.regnosys.rosetta.common.reports;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.regnosys.rosetta.common.serialisation.reportdata.ExpectedResult;
import com.rosetta.model.lib.RosettaModelObject;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

@Deprecated
public class RegReportUseCase {
	private final String useCase;
	private final String dataSetName;
	private final List<ReportField> results;
	private final RosettaModelObject useCaseReport;
	private final String useCaseReportJavaClassName;
	private final ExpectedResult expectedResults;
	private final RosettaModelObject input;
	private final Exception error;

	@JsonCreator
	public RegReportUseCase(@JsonProperty("useCase") String useCase,
							@JsonProperty("dataSetName") String dataSetName,
							@JsonProperty("results") List<ReportField> results,
							@JsonProperty("useCaseReport") RosettaModelObject useCaseReport,
							@JsonProperty("useCaseReportJavaClassName") String useCaseReportJavaClassName,
							@JsonProperty("expectedResults") ExpectedResult expectedResults,
							@JsonProperty("input") RosettaModelObject input,
							@JsonProperty("error") Exception error) {
		this.useCase = useCase;
		this.dataSetName = dataSetName;
		this.results = results;
		this.useCaseReport = useCaseReport;
		this.useCaseReportJavaClassName = useCaseReportJavaClassName;
		this.expectedResults = expectedResults;
		this.input = input;
		this.error = error;
	}

	public RegReportUseCase(RegReportUseCase other) {
		this.useCase = other.useCase;
		this.dataSetName = other.dataSetName;
		this.results = other.results;
		this.useCaseReport = other.useCaseReport;
		this.useCaseReportJavaClassName = other.useCaseReportJavaClassName;
		this.expectedResults = other.expectedResults;
		this.input = other.input;
		this.error = other.error;
	}

	public String getUseCase() {
		return useCase;
	}

	public String getDataSetName() {
		return dataSetName;
	}

	public List<ReportField> getResults() {
		return results;
	}

	public RosettaModelObject getUseCaseReport() {
		return useCaseReport;
	}

	public String getUseCaseReportJavaClassName() {
		return useCaseReportJavaClassName;
	}

	public ExpectedResult getExpectedResults() {
		return expectedResults;
	}

	public RosettaModelObject getInput() {
		return input;
	}

	public Exception getError(){ return error; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RegReportUseCase that = (RegReportUseCase) o;
		return Objects.equals(getUseCase(), that.getUseCase()) && Objects.equals(getDataSetName(), that.getDataSetName())
				&& Objects.equals(getResults(), that.getResults()) && Objects.equals(getUseCaseReport(), that.getUseCaseReport())
				&& Objects.equals(getUseCaseReportJavaClassName(), that.getUseCaseReportJavaClassName())
				&& Objects.equals(getExpectedResults(), that.getExpectedResults())
				&& Objects.equals(getInput(), that.getInput())
				&& Objects.equals(getError(), that.getError());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getUseCase(), getDataSetName(), getResults(), getUseCaseReport(), getUseCaseReportJavaClassName(), getExpectedResults(), getInput(), getError());
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", RegReportUseCase.class.getSimpleName() + "[", "]")
				.add("useCase='" + useCase + "'")
				.add("dataSetName='" + dataSetName + "'")
				.add("results=" + results)
				.add("useCaseReport=" + useCaseReport)
				.add("useCaseReportJavaClassName='" + useCaseReportJavaClassName + "'")
				.add("expectedResults=" + expectedResults)
				.add("input=" + input)
				.add("error=" + error)
				.toString();
	}
}
