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

import quickfix.CharField;
import quickfix.DataDictionary;
import quickfix.FieldMap;
import quickfix.FieldType;

import java.math.BigDecimal;
import java.util.Arrays;

public final class FIXSerializerFormatter {

    private FIXSerializerFormatter() {
    }

    public static void setField(FieldMap target, DataDictionary dictionary, int tag, Object value) {
        if (value == null) {
            return;
        }

        if (value instanceof String) {
            target.setString(tag, (String) value);
            return;
        }

        FieldType fieldType = dictionary.getFieldType(tag);

        if (value instanceof Boolean) {
            setBoolean(target, tag, fieldType, (Boolean) value);
        } else if (value instanceof BigDecimal) {
            setDecimal(target, tag, fieldType, (BigDecimal) value);
        } else if (value instanceof Character) {
            requireType(tag, fieldType, FieldType.CHAR);
            target.setField(new CharField(tag, (Character) value));
        } else {
            target.setString(tag, value.toString());
        }
    }

    private static void setBoolean(FieldMap target, int tag, FieldType fieldType, Boolean value) {
        requireType(tag, fieldType, FieldType.BOOLEAN);
        target.setBoolean(tag, value);
    }

    private static void setDecimal(FieldMap target, int tag, FieldType fieldType, BigDecimal value) {

        switch (fieldType) {
            case INT:
            case NUMINGROUP:
            case SEQNUM:
            case LENGTH:
                try {
                    target.setInt(tag, value.intValueExact());
                } catch (ArithmeticException e) {
                    throw new IllegalArgumentException(String.format("Invalid value '%s' for FIX field %s of type %s", value, tag, fieldType), e);
                }
                break;

            case PRICE:
            case AMT:
            case QTY:
            case FLOAT:
            case PRICEOFFSET:
            case PERCENTAGE:
                target.setDecimal(tag, value);
                break;

            default:
                target.setString(tag, value.toPlainString());
                break;
        }
    }

    // Validates source values against their FIX dictionary field types.
    private static void requireType(int tag, FieldType actual, FieldType... expected) {
        for (FieldType expectedType : expected) {
            if (actual == expectedType) {
                return;
            }
        }
        throw new IllegalArgumentException(String.format("FIX field %s has type %s; expected one of %s", tag, actual, Arrays.toString(expected)));
    }
}
