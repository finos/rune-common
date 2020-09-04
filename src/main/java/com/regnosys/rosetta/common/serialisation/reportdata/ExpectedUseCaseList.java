package com.regnosys.rosetta.common.serialisation.reportdata;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class ExpectedUseCaseList {
    private List<ExpectedUseCase> usecases;

    public ExpectedUseCaseList() {
    }

    public ExpectedUseCaseList(List<ExpectedUseCase> usecases) {
        this.usecases = usecases;
    }

    public List<ExpectedUseCase> getUsecases() {
        return usecases;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpectedUseCaseList that = (ExpectedUseCaseList) o;
        return Objects.equals(usecases, that.usecases);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usecases);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ExpectedUseCaseList.class.getSimpleName() + "[", "]")
                .add("usecases=" + usecases)
                .toString();
    }
}
