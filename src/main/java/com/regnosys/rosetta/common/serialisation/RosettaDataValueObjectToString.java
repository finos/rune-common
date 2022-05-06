package com.regnosys.rosetta.common.serialisation;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class RosettaDataValueObjectToString {

    public String toString(Object object) {
        if (object instanceof ZonedDateTime) {
            ZonedDateTime zonedDateTime = (ZonedDateTime) object;
            return zonedDateTime.truncatedTo(ChronoUnit.SECONDS)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        return object.toString();
    }
}
