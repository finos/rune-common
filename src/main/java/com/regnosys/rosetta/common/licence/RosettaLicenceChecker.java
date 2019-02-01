package com.regnosys.rosetta.common.licence;

import com.google.common.collect.ImmutableList;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RosettaLicenceChecker {

	public static RosettaLicence check(Path publicKeyPath, List<Path> applicationLicencePaths, String applicationName, Action callback) {
		if (publicKeyPath == null) {
			callback.checkFailed("NO PUBLIC KEY FOUND.");
			return null;
		}

		if (!Files.exists(publicKeyPath)) {
			callback.checkFailed("PUBLIC KEY '" + publicKeyPath + "' DOES NOT EXIST.");
			return null;
		}

		if (applicationLicencePaths == null) {
			callback.checkFailed("NO LICENCE FILE FOUND.");
			return null;
		}

		for (Path applicationLicencePath : applicationLicencePaths) {
			if (!Files.exists(applicationLicencePath)) {
				callback.checkFailed("LICENCE '" + applicationLicencePath + "' DOES NOT EXIST.");
				return null;
			}
		}

		List<RosettaLicence> licences = new ArrayList<>();
		for (Path applicationLicencePath : applicationLicencePaths) {
			try {
				licences.add(RosettaLicenceKey.readLicenceFile(publicKeyPath, applicationLicencePath));
			} catch (RosettaLicenceKey.RosettaLicenceException e) {
				callback.checkFailed("LICENCE ERROR " + e.getMessage());
				return null;
			}
		}

		List<RosettaLicence> matchingLicences = licences.stream().filter(l -> l.getRosettaAppName().equals(applicationName)).collect(Collectors.toList());

		if (matchingLicences.isEmpty()) {
			List<String> nonMatchingLicences = licences.stream().map(RosettaLicence::getRosettaAppName).collect(Collectors.toList());
			callback.checkFailed(
					"LICENCE FOR '" + nonMatchingLicences.stream().collect(Collectors.joining(",")) + "' CANNOT BE APPLIED TO '" + applicationName + "'.");
			return null;
		}

		List<RosettaLicence> validLicences = matchingLicences.stream()
															 .filter(l -> l.getExpiry().isAfter(LocalDate.now()) || l.getExpiry().equals(LocalDate.now()))
															 .collect(Collectors.toList());

		if (validLicences.isEmpty()) {
			List<LocalDate> nonValidLicences = matchingLicences.stream().map(RosettaLicence::getExpiry).collect(Collectors.toList());
			callback.checkFailed(
					"LICENCE FOR '" + applicationName + "' EXPIRED ON " + nonValidLicences.stream().map(Object::toString).collect(Collectors.joining(","))
							+ ".");
			return null;
		}

		Optional<RosettaLicence> longestLicence = validLicences.stream().max(Comparator.comparing(RosettaLicence::getExpiry));
		return longestLicence.get();
	}

	static Path path(String location) {
		URL systemResource = ClassLoader.getSystemResource(location);
		if (systemResource != null) {
			try {
				return Paths.get(systemResource.toURI());
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
		return Paths.get(location);
	}

	/**
	 * Decrypts the rosetta licence key and checks if it is valid.
	 *
	 * @param publicKey          Public key to decrypt the licence file
	 * @param applicationLicence Location of Rosetta Licence key.
	 * @param applicationName    Name of the application that must match the name in the licence
	 * @param callback           failure callback. Useful if you want to terminate the JVM on a failure.
	 * @return RosettaLicence object from the applicationLicence
	 */
	public static RosettaLicence check(Path publicKey, Path applicationLicence, String applicationName, Action callback) {
		return check(publicKey, ImmutableList.of(applicationLicence), applicationName, callback);
	}

	public interface Action {
		void checkFailed(String message);
	}
}
