import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Signs exact manifest bytes; passwords are read from ignored properties, never CLI arguments. */
class SignDatabaseManifest {
    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException("Usage: java scripts/SignDatabaseManifest.java MANIFEST KEYSTORE_PROPERTIES [PUBLIC_CERTIFICATE_OUTPUT]");
        }
        Path manifest = Path.of(args[0]);
        Path propertiesPath = Path.of(args[1]).toAbsolutePath();
        Properties properties = new Properties();
        try (var input = Files.newInputStream(propertiesPath)) { properties.load(input); }
        Path store = propertiesPath.getParent().resolve(properties.getProperty("STORE_FILE")).normalize();
        KeyStore keys = KeyStore.getInstance("JKS");
        char[] storePassword = properties.getProperty("STORE_PASSWORD").toCharArray();
        char[] keyPassword = properties.getProperty("KEY_PASSWORD").toCharArray();
        try {
            try (var input = Files.newInputStream(store)) { keys.load(input, storePassword); }
            String alias = properties.getProperty("KEY_ALIAS");
            var certificate = keys.getCertificate(alias);
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign((PrivateKey) keys.getKey(alias, keyPassword));
            signer.update(Files.readAllBytes(manifest));
            byte[] signature = signer.sign();
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(certificate);
            verifier.update(Files.readAllBytes(manifest));
            if (!verifier.verify(signature)) throw new IllegalStateException("Signature verification failed");
            if (args.length == 3) {
                Path output = Path.of(args[2]);
                byte[] encoded = certificate.getEncoded();
                if (Files.exists(output) && !Arrays.equals(Files.readAllBytes(output), encoded)) {
                    throw new IllegalStateException("Existing public certificate differs; refusing to replace trust key");
                }
                if (output.getParent() != null) Files.createDirectories(output.getParent());
                Files.write(output, encoded);
            }
            Files.write(manifest.resolveSibling("database-manifest.sig"), signature);
            StringBuilder checksums = new StringBuilder();
            for (String name : List.of("poetry.db.gz", "database-manifest.json", "database-manifest.sig", "validation-report.json", "NOTICE")) {
                Path asset = manifest.resolveSibling(name);
                if (!Files.isRegularFile(asset)) continue;
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                try (var input = Files.newInputStream(asset)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
                }
                checksums.append(HexFormat.of().formatHex(digest.digest())).append("  ").append(name).append("\n");
            }
            Files.writeString(manifest.resolveSibling("SHA256SUMS"), checksums);
            System.out.println("Manifest signed and verified; no private key or password exported.");
        } finally {
            Arrays.fill(storePassword, '\0');
            Arrays.fill(keyPassword, '\0');
        }
    }
}
