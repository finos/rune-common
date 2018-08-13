package com.regnosys.rosetta.common.licence;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

public class RosettaLicenceChecker {

    /**
     * Decrypts the rosetta licence key and checks if it is valid.
     * @param publicKey Public key to decrypt the licence file
     * @param rosettaIngestLicence Location of Rosetta Licence key.
     * @param applicationName Name of the application that must match the name in the licence
     * @param callback failure callback. Useful if you want to terminate the JVM on a failure.
     * @return RosettaLicence object from the rosettaIngestLicence
     * @throws IOException
     * @throws URISyntaxException
     */
    public static RosettaLicence check(String publicKey, String rosettaIngestLicence, String applicationName, Action callback) throws IOException, URISyntaxException {
        if (rosettaIngestLicence == null) {
            callback.checkFailed("NO LICENCE FILE FOUND.");
            return null;
        }
        Path rosettaIngestLicencePath = Paths.get(rosettaIngestLicence);
        if (!Files.exists(rosettaIngestLicencePath)) {
            callback.checkFailed("LICENCE '" + rosettaIngestLicencePath + "' DOES NOT EXIST.");
            return null;
        }

        try {
            RosettaLicence rosettaLicence = RosettaLicenceKey.readLicenceFile(publicKey, rosettaIngestLicencePath);

            if (!rosettaLicence.getRosettaAppName().equals(applicationName)) {
                callback.checkFailed("LICENCE FOR '" + rosettaLicence.getRosettaAppName() + "' CANNOT BE APPLIED TO '" + applicationName + "'.");
            } else if (rosettaLicence.getExpiry().isBefore(LocalDate.now()) || rosettaLicence.getExpiry().equals(LocalDate.now())) {
                callback.checkFailed("LICENCE FOR '" + rosettaLicence.getRosettaAppName() + "' EXPIRED ON " + rosettaLicence.getExpiry().toString() + ".");
            }
            return rosettaLicence;

        } catch (RosettaLicenceKey.RosettaLicenceException e) {
            callback.checkFailed("LICENCE ERROR " + e.getMessage());
            return null;
        }
    }

    public interface Action {
        void checkFailed(String message);
    }
}
