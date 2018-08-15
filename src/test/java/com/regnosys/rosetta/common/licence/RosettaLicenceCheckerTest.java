package com.regnosys.rosetta.common.licence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RosettaLicenceCheckerTest {

    @Test
    void expiredLicence() throws IOException, URISyntaxException {
        Path licence = createTempLicence("test-private-key.der", new RosettaLicence("my new client", "test-app-123", "1", LocalDate.of(2018, 01, 15)));

        RosettaLicenceChecker.check("test-public-key.der", licence.toString(), "test-app-123", error -> {
            assertEquals("LICENCE FOR 'test-app-123' EXPIRED ON 2018-01-15.", error);
        });
    }

    @Test
    void incorrectAppName() throws IOException, URISyntaxException {
        Path licence = createTempLicence("test-private-key.der", new RosettaLicence("my new client", "test-app-999", "1", LocalDate.of(2055, 01, 15)));

        RosettaLicenceChecker.check("test-public-key.der", licence.toString(), "test-app-123", error ->
                assertEquals("LICENCE FOR 'test-app-999' CANNOT BE APPLIED TO 'test-app-123'.", error));
    }

    @Test
    void missingLicenceFile() throws IOException, URISyntaxException {
        Path licence = Paths.get("foo.bar");

        RosettaLicenceChecker.check("test-public-key.der", licence.toString(), "test-app-123", error ->
                assertEquals("LICENCE 'foo.bar' DOES NOT EXIST.", error));
    }

    @Test
    void noLicenceFile() throws IOException, URISyntaxException {
        RosettaLicenceChecker.check("test-public-key.der", null, "test-app-123", error ->
                assertEquals("NO LICENCE FILE FOUND.", error));
    }

    @Test
    void junkLicenceFile() throws IOException, URISyntaxException {
        Path licence = Files.createTempFile("RosettaLicenceKeyTest", "rosetta-licence");
        Files.write(licence, "Junk".getBytes());
        RosettaLicenceChecker.check("test-public-key.der", licence.toString(), "test-app-123", error ->
                assertEquals("LICENCE ERROR Could not decrypt rosetta licence key: Decryption error", error));
    }

    private Path createTempLicence(String privateKey, RosettaLicence rosettaLicence) throws IOException, URISyntaxException {
        return RosettaLicenceKey.newLicenceFile(Paths.get(ClassLoader.getSystemResource(privateKey).toURI()),
                Files.createTempFile("RosettaLicenceKeyTest", "rosetta-licence"),
                rosettaLicence);
    }
}