package com.regnosys.rosetta.common.serialisation.reportdata;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
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

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportDataItemTest {

    @Test
    void getInputRethrowsTheLoadErrorUnwrapped() {
        IOException error = new IOException("input file missing");
        ReportDataItem item = new ReportDataItem("name", "input.json", null, error);

        IOException thrown = assertThrows(IOException.class, item::getInput);

        assertSame(error, thrown);
    }

    @Test
    void getInputReturnsTheInputWhenThereIsNoError() {
        ReportDataItem item = new ReportDataItem("name", "input.json", null);

        assertEquals("input.json", item.getInput());
    }
}
