package com.regnosys.rosetta.common.licence;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class RosettaLicenceCheckerTest {

    private static final Path TEST_PRIVATE_KEY_DER = RosettaLicenceChecker.path("test-private-key.der");
    private static final Path TEST_PUBLIC_KEY_DER = RosettaLicenceChecker.path("test-public-key.der");

    @Test
    void correctLicence() throws IOException {
        RosettaLicence expectedLic = new RosettaLicence("my new client", "test-app-123", "1", LocalDate.of(2088, 1, 15));
        Path licencePath = createTempLicence(TEST_PRIVATE_KEY_DER, expectedLic);

        RosettaLicence licence = RosettaLicenceChecker.check(TEST_PUBLIC_KEY_DER, licencePath, "test-app-123", error ->
                fail("No error expected" + error));

        assertEquals(expectedLic, licence);
    }

    @Test
    void correctMultipleLicence() throws IOException {
        RosettaLicence l1 = new RosettaLicence("my new client", "test-app-999", "1", LocalDate.of(1999, 1, 15));
        // this one has correct app name and furthest expiry
        RosettaLicence l2 = new RosettaLicence("my new client", "test-app-123", "1", LocalDate.of(2088, 1, 15));
        RosettaLicence l3 = new RosettaLicence("my new client", "test-app-777", "1", LocalDate.of(2100, 1, 15));
        RosettaLicence l4 = new RosettaLicence("my new client", "test-app-123", "1", LocalDate.of(1999, 1, 15));
        RosettaLicence l5 = new RosettaLicence("my new client", "test-app-123", "1", LocalDate.of(2022, 1, 15));

        Path l1Path = createTempLicence(TEST_PRIVATE_KEY_DER, l1);
        Path l2Path = createTempLicence(TEST_PRIVATE_KEY_DER, l2);
        Path l3Path = createTempLicence(TEST_PRIVATE_KEY_DER, l3);
        Path l4Path = createTempLicence(TEST_PRIVATE_KEY_DER, l4);
        Path l5Path = createTempLicence(TEST_PRIVATE_KEY_DER, l5);

        RosettaLicence licence = RosettaLicenceChecker.check(TEST_PUBLIC_KEY_DER, ImmutableList.of(l1Path, l2Path, l3Path, l4Path, l5Path), "test-app-123",
                error -> fail( "No error expected: " + error));

        assertEquals(l2, licence);
    }

    @Test
    void expiredLicence() throws IOException {
        Path licence = createTempLicence(TEST_PRIVATE_KEY_DER, new RosettaLicence("my new client", "test-app-123", "1", LocalDate.of(2018, 1, 15)));

        RosettaLicenceChecker.check(TEST_PUBLIC_KEY_DER, licence, "test-app-123", error ->
                assertEquals("LICENCE FOR 'test-app-123' EXPIRED ON 2018-01-15.", error));
    }

    @Test
    void incorrectAppName() throws IOException {
        Path licence = createTempLicence(TEST_PRIVATE_KEY_DER, new RosettaLicence("my new client", "test-app-999", "1", LocalDate.of(2055, 1, 15)));

        RosettaLicenceChecker.check(TEST_PUBLIC_KEY_DER, licence, "test-app-123", error ->
                assertEquals("LICENCE FOR 'test-app-999' CANNOT BE APPLIED TO 'test-app-123'.", error));
    }

    @Test
    void missingLicenceFile() {
        Path licence = Paths.get("foo.bar");

        RosettaLicenceChecker.check(TEST_PUBLIC_KEY_DER, licence, "test-app-123", error ->
                assertEquals("LICENCE 'foo.bar' DOES NOT EXIST.", error));
    }

    @Test
    void noLicenceFile() {
        RosettaLicenceChecker.check(TEST_PUBLIC_KEY_DER, (List<Path>) null, "test-app-123", error ->
                assertEquals("NO LICENCE FILE FOUND.", error));
    }

    @Test
    void junkLicenceFile() throws IOException {
        Path licence = Files.createTempFile("RosettaLicenceKeyTest", "rosetta-licence");
        Files.write(licence, "Junk".getBytes());
        RosettaLicenceChecker.check(TEST_PUBLIC_KEY_DER, licence, "test-app-123", error ->
                assertEquals("LICENCE ERROR Could not decrypt rosetta licence key: Decryption error", error));
    }

    private Path createTempLicence(Path privateKey, RosettaLicence rosettaLicence) throws IOException {
        return RosettaLicenceKey.newLicenceFile(privateKey,
                Files.createTempFile("RosettaLicenceKeyTest", "rosetta-licence"),
                rosettaLicence);
    }
}