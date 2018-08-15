package com.regnosys.rosetta.common.licence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RosettaLicenceKeyTest {

    private Path privateKey;
    private Path publicKey;

    private static RosettaLicence ROSETTA_LICENCE = new RosettaLicence("My Client", "My Awesome App", "1.0.0", LocalDate.of(1981, 4, 13));
    private static String ROSETTA_LICENCE_KEY =
            "rwAMUSpDdmYxp2or+cxOC/zvEYHdOy\n" +
                    "L5+zKEDDGkVQR+Iz8IBMCztb1Z8lu7\n" +
                    "ITFOCXSsKq5wg1BgIlKBHeoa39El78\n" +
                    "xMNYwXY55z4+ggkRwhpdUkpOXJdziz\n" +
                    "T2dh0G0WSLmZsIHROGi7HRYUEA+gVz\n" +
                    "k4SgVlu8SdPlPR8h4rYKQYj5tntWa+\n" +
                    "kjq6cCcIvf58DbarNm4cy8GJlcaexb\n" +
                    "zp4C42ZnmmZlNxklVh9mNY+PFlMRzb\n" +
                    "2MTkPZumX3EyVaHxNHJANt6b2hKvt3\n" +
                    "uzsDb8QcwR3EaT0zzz+S7sG53pnKCO\n" +
                    "VLTZFLtM4c/4yozkpqtfWPoq4QD2NO\n" +
                    "OIMtGKavgJ6Q==";

    @BeforeEach
    void setUp() throws URISyntaxException {
        privateKey = Paths.get(ClassLoader.getSystemResource("test-private-key.der").toURI());
        publicKey = Paths.get(ClassLoader.getSystemResource("test-public-key.der").toURI());

    }

    @Test
    void checkEncryptLicenceKey() {
        String encrypted = RosettaLicenceKey.encrypt(privateKey, ROSETTA_LICENCE);
        assertEquals(ROSETTA_LICENCE_KEY, encrypted);
    }

    @Test
    void checkDecryptLicenceKey() {
        RosettaLicence decrypted = RosettaLicenceKey.decrypt(publicKey, ROSETTA_LICENCE_KEY);
        assertEquals(ROSETTA_LICENCE, decrypted);
    }

    @Test
    void readAndWriteLicence() throws IOException {
        Path licenceKeyPath = Files.createTempFile("RosettaLicenceKeyTest", "rosetta-licence");
        RosettaLicenceKey.newLicenceFile(privateKey, licenceKeyPath, ROSETTA_LICENCE);
        System.out.println(new String(Files.readAllBytes(licenceKeyPath)));
        RosettaLicence rosettaLicence = RosettaLicenceKey.readLicenceFile(publicKey, licenceKeyPath);
        assertEquals(ROSETTA_LICENCE, rosettaLicence);
    }
}