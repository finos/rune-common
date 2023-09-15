package com.regnosys.rosetta.common.serialisation.mixin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rosetta.model.lib.records.DateImpl;

import java.time.LocalDate;

public class DateExtended extends DateImpl {
    public DateExtended(@JsonProperty("year") int year, @JsonProperty("month") int month, @JsonProperty("day") int day) {
        super(year, month, day);
    }

    public DateExtended(String date) {
        super(LocalDate.parse(date));
    }
}
