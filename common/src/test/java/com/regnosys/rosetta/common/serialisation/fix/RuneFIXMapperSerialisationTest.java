package com.regnosys.rosetta.common.serialisation.fix;

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

import com.regnosys.rosetta.common.serialisation.fix.processor.RuneFIXSerializerIssue;
import com.regnosys.rosetta.common.serialisation.fix.processor.RuneFIXSerializerReport;
import csv.test.user.User;
import fix.test.trade.FixInstrument;
import fix.test.trade.FixPartiallyLabelledReport;
import fix.test.trade.FixParty;
import fix.test.trade.FixSide;
import fix.test.trade.FixSideEnum;
import fix.test.trade.FixTradeCaptureReport;
import fix.test.trade.FixTradePriceConditionEnum;
import fix.test.trade.FixUnknownLabelReport;
import fix.test.trade.FixUnlabelledReport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.FieldNotFound;
import quickfix.Group;
import quickfix.Message;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for {@link RuneFIXMapper} against generated Rune types and a real QuickFIX/J
 * {@link DataDictionary}, with no mocks.
 *
 * <p>The model in {@code rosetta/fix-trade-capture-report-type.rosetta} mirrors the shape of a
 * MarketAxess MiFIR {@code TradeCaptureReport}: labelled scalars, a nested component whose fields
 * belong to the parent message, a multi-cardinality enum and a complex type that both become FIX
 * repeating groups, and a repeating group nested in another. The dictionary in
 * {@code serialisation/fix/FIX50SP2_TradeCaptureReport_Test.xml} defines just those fields, with
 * their FIX 5.0 SP2 tags and types.
 *
 * <p>Assertions compare the message body (everything after {@code 35=AE} and before the
 * {@code 10=} checksum), with SOH shown as {@code |}. QuickFIX/J writes body fields in tag order
 * and repeating groups after them; within a group, entries follow the dictionary's field order.
 */
class RuneFIXMapperSerialisationTest {

    private static final String CONFIG_PATH = "serialisation/fix/fix-test-config.json";

    private static RuneFIXConfiguration configuration;
    private static DataDictionary dictionary;
    private static RuneFIXMapper mapper;

    @BeforeAll
    static void loadMapper() throws IOException, ConfigError {
        try (InputStream config = resource(CONFIG_PATH)) {
            configuration = RuneFIXConfiguration.load(config);
        }
        try (InputStream dictionaryXml = resource(configuration.getDictionaryPath())) {
            dictionary = new DataDictionary(dictionaryXml);
        }
        mapper = RuneFIXMapper.createFIXMapper(configuration, dictionary);
    }

    // ---------------------------------------------------------------------------
    // Scalars
    // ---------------------------------------------------------------------------

    @Test
    void shouldWriteMsgTypeFromConfiguredRouting() {
        String fix = mapper.writeValueAsString(minimalReport());

        assertTrue(fix.contains("35=AE\u0001"), fix);
    }

    @Test
    void shouldWriteOnlySetFieldsWhenOptionalAttributesAreAbsent() {
        assertEquals("571=TR-1|", body(mapper.writeValueAsString(minimalReport())));
    }

    @Test
    void shouldWriteEachScalarTypeInItsFixRepresentation() {
        FixTradeCaptureReport report = FixTradeCaptureReport.builder()
                .setTradeReportID("TR-1")
                .setTotNumTradeReports(3)
                .setLastPx(new BigDecimal("99.125"))
                .setLastQty(new BigDecimal("1000000"))
                .setPreviouslyReported(true)
                .setTransactTime("20261006-09:30:00.123")
                .build();

        assertEquals("31=99.125|32=1000000|60=20261006-09:30:00.123|570=Y|571=TR-1|748=3|",
                body(mapper.writeValueAsString(report)));
    }

    @Test
    void shouldWriteDecimalInPlainNotationWhenValueHasAnExponent() {
        FixTradeCaptureReport report = minimalReport().toBuilder()
                .setLastPx(new BigDecimal("1E+3"))
                .build();

        assertEquals("31=1000|571=TR-1|", body(mapper.writeValueAsString(report)));
    }

    @Test
    void shouldWriteBooleanFalseAsN() {
        FixTradeCaptureReport report = minimalReport().toBuilder()
                .setPreviouslyReported(false)
                .build();

        assertEquals("570=N|571=TR-1|", body(mapper.writeValueAsString(report)));
    }

    // ---------------------------------------------------------------------------
    // Components and repeating groups
    // ---------------------------------------------------------------------------

    @Test
    void shouldFlattenNestedComponentIntoTheMessageBody() {
        FixTradeCaptureReport report = minimalReport().toBuilder()
                .setInstrument(FixInstrument.builder()
                        .setSecurityID("GB00B03MLX29")
                        .setCurrency("EUR"))
                .build();

        assertEquals("15=EUR|48=GB00B03MLX29|571=TR-1|", body(mapper.writeValueAsString(report)));
    }

    @Test
    void shouldWriteEnumListAsRepeatingGroupOfDisplayNames() {
        FixTradeCaptureReport report = minimalReport().toBuilder()
                .addTradePriceCondition(FixTradePriceConditionEnum.SPECIAL_CUM_DIVIDEND)
                .addTradePriceCondition(FixTradePriceConditionEnum.SPECIAL_CUM_RIGHTS)
                .build();

        assertEquals("571=TR-1|1838=2|1839=0|1839=1|", body(mapper.writeValueAsString(report)));
    }

    @Test
    void shouldWriteComplexListAsRepeatingGroupWithDelimiterFirst() {
        FixTradeCaptureReport report = minimalReport().toBuilder()
                .addSide(FixSide.builder().setSide(FixSideEnum.BUY))
                .addSide(FixSide.builder().setSide(FixSideEnum.SELL))
                .build();

        assertEquals("571=TR-1|552=2|54=1|54=2|", body(mapper.writeValueAsString(report)));
    }

    @Test
    void shouldNestRepeatingGroupInsideItsParentGroupEntry() throws FieldNotFound {
        FixTradeCaptureReport report = minimalReport().toBuilder()
                .addSide(FixSide.builder()
                        .setSide(FixSideEnum.BUY)
                        .addParty(FixParty.builder().setPartyID("BUYER").setPartyRole(1))
                        .addParty(FixParty.builder().setPartyID("BROKER").setPartyRole(7)))
                .addSide(FixSide.builder()
                        .setSide(FixSideEnum.SELL)
                        .addParty(FixParty.builder().setPartyID("SELLER").setPartyRole(1)))
                .build();

        Message message = mapper.writeValueAsFIXMessage(report);

        assertEquals(2, message.getInt(552));
        Group buySide = message.getGroup(1, 552);
        assertEquals('1', buySide.getChar(54));
        assertEquals(2, buySide.getInt(453));
        assertEquals("BUYER", buySide.getGroup(1, 453).getString(448));
        assertEquals("BROKER", buySide.getGroup(2, 453).getString(448));
        assertEquals(7, buySide.getGroup(2, 453).getInt(452));

        Group sellSide = message.getGroup(2, 552);
        assertEquals('2', sellSide.getChar(54));
        assertEquals(1, sellSide.getInt(453));
        assertEquals("SELLER", sellSide.getGroup(1, 453).getString(448));

        assertEquals("571=TR-1|552=2|54=1|453=2|448=BUYER|452=1|448=BROKER|452=7|54=2|453=1|448=SELLER|452=1|",
                body(message.toString()));
    }

    @Test
    void shouldOmitRepeatingGroupWhenListIsEmpty() {
        FixTradeCaptureReport report = minimalReport().toBuilder()
                .addSide(FixSide.builder().setSide(FixSideEnum.BUY))
                .build();

        String fix = mapper.writeValueAsString(report);

        assertFalse(fix.contains("453="), fix);
        assertFalse(fix.contains("1838="), fix);
    }

    // ---------------------------------------------------------------------------
    // Validation against the dictionary
    // ---------------------------------------------------------------------------

    @Test
    void shouldReportNoIssuesWhenValidatedMessageMatchesDictionary() {
        RuneFIXSerializerReport report = mapper.writeValueAsFIXReport(fullReport(), true);

        assertTrue(report.getIssues().isEmpty(), () -> describe(report.getIssues()));
    }

    @Test
    @Disabled("RuneFIXSerializerProcessor.validateBodyOnlyReport collects the validation failure in "
            + "reportIssues but returns issues, so the failure never reaches the report")
    void shouldReportValidationFailureWhenRequiredGroupIsMissing() {
        // NoSides is required="Y" in the test dictionary, and minimalReport() has no sides.
        RuneFIXSerializerReport report = mapper.writeValueAsFIXReport(minimalReport(), true);

        assertTrue(report.hasErrors(), "Expected a validation error for the missing NoSides group");
        assertEquals(RuneFIXSerializerIssue.Code.FIX_VALIDATION_FAILED, report.getErrors().get(0).getCode());
    }

    @Test
    void shouldNotValidateByDefault() {
        RuneFIXSerializerReport report = mapper.writeValueAsFIXReport(minimalReport());

        assertFalse(report.hasErrors(), () -> describe(report.getIssues()));
    }

    // ---------------------------------------------------------------------------
    // Unmappable attributes
    // ---------------------------------------------------------------------------

    @Test
    void shouldReportMissingLabelAndStillWriteLabelledFields() {
        FixPartiallyLabelledReport value = FixPartiallyLabelledReport.builder()
                .setTradeReportID("TR-1")
                .setNotLabelled("ignored")
                .build();

        RuneFIXSerializerReport report = mapper.writeValueAsFIXReport(value);

        assertEquals("571=TR-1|", body(report.getFixMessage().toString()));
        assertEquals(1, report.getErrors().size(), () -> describe(report.getIssues()));
        RuneFIXSerializerIssue issue = report.getErrors().get(0);
        assertEquals(RuneFIXSerializerIssue.Code.MISSING_LABEL_MAPPING, issue.getCode());
        assertEquals("FixPartiallyLabelledReport.notLabelled", issue.getPath());
    }

    @Test
    void shouldReportMissingFixTagWhenLabelIsNotInDictionary() {
        FixUnknownLabelReport value = FixUnknownLabelReport.builder()
                .setTradeReportID("TR-1")
                .setUnknown("ignored")
                .build();

        RuneFIXSerializerReport report = mapper.writeValueAsFIXReport(value);

        assertEquals("571=TR-1|", body(report.getFixMessage().toString()));
        assertEquals(1, report.getErrors().size(), () -> describe(report.getIssues()));
        RuneFIXSerializerIssue issue = report.getErrors().get(0);
        assertEquals(RuneFIXSerializerIssue.Code.MISSING_FIX_TAG, issue.getCode());
        assertTrue(issue.getMessage().contains("NotAFixField"), issue.getMessage());
    }

    @Test
    @Disabled("Throws NullPointerException from RuneFIXSerializerProcessor: RuneFIXMapper does not handle "
            + "LabelProviderResolver.fromType returning null for a type with no labels of its own")
    void shouldThrowNamingTheTypeWhenItHasNoLabelProvider() {
        FixUnlabelledReport value = FixUnlabelledReport.builder().setTradeReportID("TR-1").build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> mapper.writeValueAsString(value));
        assertTrue(exception.getMessage().contains(FixUnlabelledReport.class.getName()), exception.getMessage());
    }

    @Test
    void shouldThrowWhenNoMsgTypeIsRoutedForTheType() {
        User user = User.builder().setUsername("u").setIdentifier("i").setFirstName("f").setLastName("l").build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> mapper.writeValueAsString(user));
        assertTrue(exception.getMessage().contains(User.class.getName()), exception.getMessage());
    }

    // ---------------------------------------------------------------------------
    // Entry points
    // ---------------------------------------------------------------------------

    @Test
    void shouldReturnNullFromEveryWriteMethodForNullValue() {
        assertNull(mapper.writeValueAsString(null));
        assertNull(mapper.writeValueAsFIXMessage(null));
        assertNull(mapper.writeValueAsFIXReport(null));
    }

    @Test
    void shouldRejectValueThatIsNotARuneModelObject() {
        assertThrows(IllegalArgumentException.class, () -> mapper.writeValueAsString("not a model object"));
    }

    @Test
    void shouldProduceTheSameMessageFromEveryWriteMethod() {
        FixTradeCaptureReport report = fullReport();

        String fromString = mapper.writeValueAsString(report);
        String fromMessage = mapper.writeValueAsFIXMessage(report).toString();
        String fromReport = mapper.writeValueAsFIXReport(report).getFixMessage().toString();

        assertEquals(fromString, fromMessage);
        assertEquals(fromString, fromReport);
    }

    @Test
    void shouldProduceIdenticalOutputWhenOneMapperIsSharedAcrossThreads() throws Exception {
        FixTradeCaptureReport report = fullReport();
        String expected = mapper.writeValueAsString(report);

        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<String>> results = executor.invokeAll(IntStream.range(0, 200)
                    .<Callable<String>>mapToObj(i -> () -> mapper.writeValueAsString(report))
                    .collect(Collectors.toList()));
            Set<String> distinct = new HashSet<>();
            for (Future<String> result : results) {
                distinct.add(result.get());
            }
            assertEquals(1, distinct.size());
            assertEquals(expected, distinct.iterator().next());
        } finally {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private static FixTradeCaptureReport minimalReport() {
        return FixTradeCaptureReport.builder()
                .setTradeReportID("TR-1")
                .build();
    }

    private static FixTradeCaptureReport fullReport() {
        return FixTradeCaptureReport.builder()
                .setTradeReportID("TR-1")
                .setTotNumTradeReports(1)
                .setLastPx(new BigDecimal("99.125"))
                .setLastQty(new BigDecimal("1000000"))
                .setPreviouslyReported(false)
                .setTransactTime("20261006-09:30:00.123")
                .addTradePriceCondition(FixTradePriceConditionEnum.SPECIAL_CUM_DIVIDEND)
                .setInstrument(FixInstrument.builder().setSecurityID("GB00B03MLX29").setCurrency("EUR"))
                .addSide(FixSide.builder()
                        .setSide(FixSideEnum.BUY)
                        .addParty(FixParty.builder().setPartyID("BUYER").setPartyRole(1)))
                .build();
    }

    /** The message body between {@code 35=<MsgType>} and the {@code 10=} checksum, with SOH as {@code |}. */
    private static String body(String fix) {
        String readable = fix.replace('\u0001', '|');
        int start = readable.indexOf("|", readable.indexOf("35=")) + 1;
        int end = readable.lastIndexOf("10=");
        return readable.substring(start, end);
    }

    private static String describe(List<RuneFIXSerializerIssue> issues) {
        return issues.stream()
                .map(issue -> issue.getCode() + " at '" + issue.getPath() + "': " + issue.getMessage())
                .collect(Collectors.joining("\n"));
    }

    private static InputStream resource(String path) {
        InputStream stream = RuneFIXMapperSerialisationTest.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Test resource not found: " + path);
        }
        return stream;
    }
}
