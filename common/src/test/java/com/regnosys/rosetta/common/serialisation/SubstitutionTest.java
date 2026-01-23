package com.regnosys.rosetta.common.serialisation;

import org.junit.jupiter.api.Test;

public class SubstitutionTest {

    @Test
    void test() {

        // use  EnvironmentalPhysicalLeg as an example from fiml-5-4-xml-config.json to build the config
        // use real xml names
        // have two tests one with xxx:foo and one without that uses the default namespace, it should populate with the correct type
        String xml = """
                <root
                xmlns="http://www.bnpparibas.com/2012/FiML-5"
                xmlns:xxx="http://www.bnpparibas.com/2012/FiML-5">
                    <bar xmlns="http://www.fpml.org/FpML-5/recordkeeping">
                        <xxx:foo>
                            <attr>C</attr>
                        </xxx:foo>
                    </bar>
                </root>
                """;

        String config = """
                    when xxx then use Foo1
                    when not xxx then use Foo2
                """;

        String rune = """
                    enum ZapEnum:
                       A
                       B

                    type Bar:
                        foo CommonFoo (1..1)

                    type Foo1 extends CommonFoo:
                        attr ZapEnum (1..1)

                    type Foo2 extends CommonFoo:
                        attr string (1..1)

                    type CommonFoo:
                        foo string (1..1)
                """;

        String expectedJsonSeralisedRune = """
                    {
                      bar: {
                      }
                    }
                """;
    }
}
