package com.regnosys.rosetta.common.licence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

import static java.nio.file.Files.readAllBytes;

/**
 * To gen a public/private key pair - use the following command:
 * <p>
 * $ openssl genrsa -out keypair.pem 2048 && openssl rsa -in keypair.pem -outform DER -pubout -out public.der && openssl pkcs8 -topk8 -nocrypt -in keypair.pem -outform DER -out private.der
 */
public class RosettaLicenceKey {

    private static final String CIPHER = "RSA/ECB/PKCS1Padding";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);


    public static void main(String[] args) throws IOException {
        if (args.length != 5) {
            System.out.println("Usage: RosettaLicenceKey <private-key> <client-id> <rosetta-app-name>, <rosetta-app-version>, <rosetta-app-expiry>");
            System.out.println("Example: java com.regnosys.rosetta.licence.RosettaLicenceKey scripts/rosetta-licences/rosetta-licence-private-key.der 'the client name' rosetta-ingest 1.0.0 2018-12-31");
            System.exit(1);
        }
        String privateKey = args[0];
        String client = args[1];
        String appName = args[2];
        String appVersion = args[3];
        String expiry = args[4];

        RosettaLicence rosettaLicence = new RosettaLicence(client, appName, appVersion, LocalDate.parse(expiry));

        Path outFile = Paths.get(client.replaceAll("[^a-z0-9]", "_").toLowerCase() + ".rosetta-licence");
        newLicenceFile(Paths.get(privateKey), outFile, rosettaLicence);
        System.out.println("Created " + outFile);
    }

    public static Path newLicenceFile(Path privateKeyPath, Path output, RosettaLicence rosettaLicence) {
        try {
            return Files.write(output, encrypt(privateKeyPath, rosettaLicence).getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static RosettaLicence readLicenceFile(Path publicKeyPath, Path input) {
        try {
            return decrypt(publicKeyPath, new String(Files.readAllBytes(input)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String encrypt(Path privateKeyPath, RosettaLicence rosettaLicence) {
        try {
            final PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(readAllBytes(privateKeyPath)));
            final byte[] jsonLicence = MAPPER.writeValueAsBytes(rosettaLicence);
            final Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            final byte[] encryptLicence = cipher.doFinal(jsonLicence);
            String base64Encoded = Base64.getEncoder().encodeToString(encryptLicence);
            return Arrays.stream(base64Encoded.split("(?<=\\G.{30})")).collect(Collectors.joining("\n"));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidKeySpecException | IOException e) {
            throw new RosettaLicenceException("Could not encrypt rosetta licence " + rosettaLicence + ": " + e.getMessage(), e);
        }
    }

    static RosettaLicence decrypt(Path publicKeyPath, String rosettaLicenceKey) {
        try {
            byte[] decoded = Base64.getDecoder().decode(rosettaLicenceKey.replace("\n", ""));
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(readAllBytes(publicKeyPath)));
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            byte[] jsonLicence = cipher.doFinal(decoded);
            return MAPPER.readValue(jsonLicence, RosettaLicence.class);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidKeySpecException | IOException e) {
            throw new RosettaLicenceException("Could not decrypt rosetta licence key: " + e.getMessage(), e);
        }
    }

    static class RosettaLicenceException extends RuntimeException {

        public RosettaLicenceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
