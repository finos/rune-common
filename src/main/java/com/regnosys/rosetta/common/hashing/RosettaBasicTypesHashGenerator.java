package com.regnosys.rosetta.common.hashing;

/*-
 * #%L
 * Rune Common
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import com.rosetta.model.lib.records.Date;

/**
 * For each basic type in Rosetta, specify a handler for the generation of a hashcode
 * @param <T> the object representing hashcode
 */
public abstract class RosettaBasicTypesHashGenerator<T> {

    public <U> T generate(U object) {
        if (object instanceof String) {
            return handle((String) object);
        }
        else if (object instanceof Integer) {
            return handle((Integer) object);
        }
        else if (object instanceof LocalDate) {
            return handle((LocalDate) object);
        }
        else if (object instanceof Date) {
        	return handle(((Date) object).toLocalDate());
        }
        else if (object instanceof LocalTime) {
            return handle((LocalTime) object);
        }
        else if (object instanceof LocalDateTime) {
            return handle((LocalDateTime) object);
        }
        else if (object instanceof ZonedDateTime) {
            return handle((ZonedDateTime) object);
        }
        else if (object instanceof BigDecimal) {
            return handle((BigDecimal) object);
        }
        else if (object instanceof Boolean) {
            return handle((Boolean) object);
        }
        else if (object instanceof Enum) {
            return handle((Enum<?>) object);
        }

        throw new IllegalArgumentException("Unsupported type: " + object.getClass());
    }

    abstract T handle(String string);

    abstract T handle(Integer integer);

    abstract T handle(LocalDate localDate);

    abstract T handle(LocalTime localTime);

    abstract T handle(LocalDateTime localDateTime);

    abstract T handle(ZonedDateTime zonedDateTime);
    
    abstract T handle(BigDecimal bigDecimal);

    abstract T handle(Boolean bool);

    abstract T handle(Enum<?> e);

}
