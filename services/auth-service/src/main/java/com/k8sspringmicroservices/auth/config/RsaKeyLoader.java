package com.k8sspringmicroservices.auth.config;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class RsaKeyLoader {

  private static final String PEM_HEADER_FOOTER_PATTERN = "-----(BEGIN|END)[A-Z ]*-----";

  private final ResourcePatternResolver resourceResolver =
      new PathMatchingResourcePatternResolver();

  public PrivateKey loadPrivateKey(String location) {
    try {
      byte[] decoded = readPemBytes(location);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new IllegalStateException("Could not load RSA private key from " + location, e);
    }
  }

  public PublicKey loadPublicKey(String location) {
    try {
      byte[] decoded = readPemBytes(location);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePublic(new X509EncodedKeySpec(decoded));
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new IllegalStateException("Could not load RSA public key from " + location, e);
    }
  }

  private byte[] readPemBytes(String location) throws IOException {
    try (InputStream inputStream = resourceResolver.getResource(location).getInputStream()) {
      String pem = new String(inputStream.readAllBytes());
      String base64 = pem.replaceAll(PEM_HEADER_FOOTER_PATTERN, "").replaceAll("\\s", "");
      return Base64.getDecoder().decode(base64);
    }
  }
}
