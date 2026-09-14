//package com.school.library.security.jwt;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.security.KeyFactory;
//import java.security.PrivateKey;
//import java.security.PublicKey;
//import java.security.spec.PKCS8EncodedKeySpec;
//import java.security.spec.X509EncodedKeySpec;
//import java.util.Base64;
//
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.core.io.Resource;
//
//public final class PemUtils {
//
//    private PemUtils() {
//    }
//
//    public static PrivateKey readPrivateKey(String location) {
//
//        try {
//            byte[] keyBytes = readPem(location);
//
//            PKCS8EncodedKeySpec keySpec =
//                    new PKCS8EncodedKeySpec(keyBytes);
//
//            KeyFactory keyFactory =
//                    KeyFactory.getInstance("RSA");
//
//            return keyFactory.generatePrivate(keySpec);
//
//        } catch (Exception e) {
//            throw new IllegalStateException(
//                    "Unable to load RSA private key: " + location,
//                    e
//            );
//        }
//    }
//
//    public static PublicKey readPublicKey(String location) {
//
//        try {
//            byte[] keyBytes = readPem(location);
//
//            X509EncodedKeySpec keySpec =
//                    new X509EncodedKeySpec(keyBytes);
//
//            KeyFactory keyFactory =
//                    KeyFactory.getInstance("RSA");
//
//            return keyFactory.generatePublic(keySpec);
//
//        } catch (Exception e) {
//            throw new IllegalStateException(
//                    "Unable to load RSA public key: " + location,
//                    e
//            );
//        }
//    }
//
//    private static byte[] readPem(String location)
//            throws IOException {
//
//        String pem;
//
//        if (location.startsWith("classpath:")) {
//
//            String resourcePath =
//                    location.substring("classpath:".length());
//
//            Resource resource =
//                    new ClassPathResource(resourcePath);
//
//            pem = new String(
//                    resource.getInputStream().readAllBytes(),
//                    StandardCharsets.UTF_8
//            );
//
//        } else if (location.startsWith("file:")) {
//
//            Path path = Path.of(
//                    location.substring("file:".length())
//            );
//
//            pem = Files.readString(path);
//
//        } else {
//
//            pem = Files.readString(Path.of(location));
//        }
//
//        pem = pem
//                .replace("-----BEGIN PRIVATE KEY-----", "")
//                .replace("-----END PRIVATE KEY-----", "")
//                .replace("-----BEGIN PUBLIC KEY-----", "")
//                .replace("-----END PUBLIC KEY-----", "")
//                .replaceAll("\\s+", "");
//
//        return Base64.getDecoder().decode(pem);
//    }
//}
//
