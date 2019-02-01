package com.regnosys.rosetta.common.licence;

import java.time.LocalDate;
import java.util.Objects;

/**
 * RosettaLicence class to hold info licence for in instance of a rosetta app.
 */
public class RosettaLicence {

	private String clientId;
	private String rosettaAppName;
	private String version;
	private LocalDate expiry;

	@SuppressWarnings("unused") // jackson
	public RosettaLicence() {
	}

	public RosettaLicence(String clientId, String rosettaAppName, String version, LocalDate expiry) {
		this.clientId = clientId;
		this.rosettaAppName = rosettaAppName;
		this.version = version;
		this.expiry = expiry;
	}

	public String getClientId() {
		return clientId;
	}

	public String getRosettaAppName() {
		return rosettaAppName;
	}

	public String getVersion() {
		return version;
	}

	public LocalDate getExpiry() {
		return expiry;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		RosettaLicence that = (RosettaLicence) o;
		return Objects.equals(clientId, that.clientId) &&
				Objects.equals(rosettaAppName, that.rosettaAppName) &&
				Objects.equals(version, that.version) &&
				Objects.equals(expiry, that.expiry);
	}

	@Override
	public int hashCode() {

		return Objects.hash(clientId, rosettaAppName, version, expiry);
	}

	@Override
	public String
	toString() {
		return "RosettaLicence{" +
				"clientId='" + clientId + '\'' +
				", rosettaAppName='" + rosettaAppName + '\'' +
				", version='" + version + '\'' +
				", expiry=" + expiry +
				'}';
	}
}

