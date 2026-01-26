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

public class SubstitutionTest {

//    @Test
//    void test() {
//
//        // use  EnvironmentalPhysicalLeg as an example from fiml-5-4-xml-config.json to build the config
//        // use real xml names
//        // have two tests one with xxx:foo and one without that uses the default namespace, it should populate with the correct type
//        String xml = """
//                <root
//                xmlns="http://www.bnpparibas.com/2012/FiML-5"
//                xmlns:xxx="http://www.bnpparibas.com/2012/FiML-5">
//                    <bar xmlns="http://www.fpml.org/FpML-5/recordkeeping">
//                        <xxx:foo>
//                            <attr>C</attr>
//                        </xxx:foo>
//                    </bar>
//                </root>
//                """;
//
//        String config = """
//                    when xxx then use Foo1
//                    when not xxx then use Foo2
//                """;
//
//        String rune = """
//                    enum ZapEnum:
//                       A
//                       B
//
//                    type Bar:
//                        foo CommonFoo (1..1)
//
//                    type Foo1 extends CommonFoo:
//                        attr ZapEnum (1..1)
//
//                    type Foo2 extends CommonFoo:
//                        attr string (1..1)
//
//                    type CommonFoo:
//                        foo string (1..1)
//                """;
//
//        String expectedJsonSeralisedRune = """
//                    {
//                      bar: {
//                      }
//                    }
//                """;
//    }
}
