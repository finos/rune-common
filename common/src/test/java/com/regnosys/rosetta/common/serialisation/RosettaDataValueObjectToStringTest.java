package com.regnosys.rosetta.common.serialisation;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2026 REGnosys
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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.ZoneId;

public class RosettaDataValueObjectToStringTest {

    @Test
    public void testZonedDateTimeToString() {
        // Create a ZonedDateTime with milliseconds
        ZonedDateTime dateTime = ZonedDateTime.of(2023, 5, 15, 10, 30, 45, 123000000, ZoneId.of("UTC"));
        
        // Expected result should have seconds precision (no milliseconds) and ISO format
        String expected = "2023-05-15T10:30:45Z";
        String result = RosettaDataValueObjectToString.toValueString(dateTime);
        
        assertEquals(expected, result, "ZonedDateTime should be formatted with seconds precision in ISO format");
    }
    
    @Test
    public void testBigDecimalToString() {
        // Test with a regular decimal
        BigDecimal decimal1 = new BigDecimal("123.45");
        assertEquals("123.45", RosettaDataValueObjectToString.toValueString(decimal1));
        
        // Test with scientific notation
        BigDecimal decimal2 = new BigDecimal("1.23E+3");
        assertEquals("1230", RosettaDataValueObjectToString.toValueString(decimal2), 
                "BigDecimal should be converted to plain string without scientific notation");
        
        // Test with trailing zeros
        BigDecimal decimal3 = new BigDecimal("100.00");
        assertEquals("100.00", RosettaDataValueObjectToString.toValueString(decimal3),
                "BigDecimal should preserve trailing zeros");
    }
    
    @Test
    public void testOtherObjectsToString() {
        // Test with String
        String str = "test string";
        assertEquals("test string", RosettaDataValueObjectToString.toValueString(str));
        
        // Test with Integer
        Integer num = 42;
        assertEquals("42", RosettaDataValueObjectToString.toValueString(num));
        
        // Test with Boolean
        Boolean bool = true;
        assertEquals("true", RosettaDataValueObjectToString.toValueString(bool));
    }
}
