package com.regnosys.rosetta.common.reports;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

@Deprecated
public class RegReport {
	private final RegReportIdentifier identifier;
	private final List<RegReportUseCase> useCases;

	@JsonCreator
	public RegReport(@JsonProperty("identifier") RegReportIdentifier identifier,
					 @JsonProperty("useCases") List<RegReportUseCase> useCases) {
		this.identifier = identifier;
		this.useCases = useCases;
	}

	public RegReport(RegReport other) {
		this.identifier = other.identifier;
		this.useCases = other.useCases;
	}

	public RegReportIdentifier getIdentifier() {
		return identifier;
	}

	public List<RegReportUseCase> getUseCases() {
		return useCases;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RegReport regReport = (RegReport) o;
		return Objects.equals(getIdentifier(), regReport.getIdentifier()) && Objects.equals(getUseCases(), regReport.getUseCases());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getIdentifier(), getUseCases());
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", RegReport.class.getSimpleName() + "[", "]")
				.add("identifier=" + identifier)
				.add("useCases=" + useCases)
				.toString();
	}
}