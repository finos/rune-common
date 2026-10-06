package com.regnosys.rosetta.common.serialisation.fix.processor;

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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import quickfix.DataDictionary;
import quickfix.FieldType;
import quickfix.Message;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FIXSerializerFormatterTest {

    @Mock
    private DataDictionary dictionary;

    @Test
    void setField_NullValue_DoesNothing() {
        Message target = new Message();
        FIXSerializerFormatter.setField(target, dictionary, 100, null);
        assertFalse(target.isSetField(100));
        verifyNoInteractions(dictionary);
    }

    @Test
    void setField_String_PreservesValueWithoutFormatting() throws Exception {
        Message target = new Message();
        FIXSerializerFormatter.setField(target, dictionary, 60, "20261002-12:30:00.000");
        assertEquals("20261002-12:30:00.000", target.getString(60));
        verifyNoInteractions(dictionary);
    }

    @Test
    void setField_Boolean_UsesQuickFixBooleanRepresentation() throws Exception {
        when(dictionary.getFieldType(140)).thenReturn(FieldType.BOOLEAN);
        Message target = new Message();
        FIXSerializerFormatter.setField(target, dictionary, 140, true);
        assertEquals("Y", target.getString(140));
    }

    @Test
    void setField_Decimal_UsesDecimalField() throws Exception {
        when(dictionary.getFieldType(44)).thenReturn(FieldType.PRICE);
        Message target = new Message();
        FIXSerializerFormatter.setField(target, dictionary, 44, new BigDecimal("1E+3"));
        assertEquals("1000", target.getString(44));
    }

    @Test
    void setField_IntegerDecimal_UsesIntegerField()
            throws Exception {
        when(dictionary.getFieldType(34)).thenReturn(FieldType.SEQNUM);
        Message target = new Message();
        FIXSerializerFormatter.setField(target, dictionary, 34, new BigDecimal("1000.0"));
        assertEquals("1000", target.getString(34));
    }

    @Test
    void setField_NonIntegralInteger_ThrowsException() {
        when(dictionary.getFieldType(34)).thenReturn(FieldType.SEQNUM);
        Message target = new Message();
        assertThrows(IllegalArgumentException.class,
                () -> FIXSerializerFormatter.setField(
                        target,
                        dictionary,
                        34,
                        new BigDecimal("1000.5"))
        );
    }

    @Test
    void setField_BooleanWithWrongFixType_ThrowsException() {
        when(dictionary.getFieldType(140)).thenReturn(FieldType.STRING);
        Message target = new Message();
        assertThrows(IllegalArgumentException.class,
                () -> FIXSerializerFormatter.setField(
                        target,
                        dictionary,
                        140,
                        true)
        );
    }
}
