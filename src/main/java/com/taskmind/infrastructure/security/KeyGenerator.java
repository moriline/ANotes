package com.taskmind.infrastructure.security;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

@ApplicationScoped
public class KeyGenerator {

    @PostConstruct
    public void generateKeys() throws Exception {
        Path resourcesDir = Path.of("src/main/resources/META-INF/resources");
        Files.createDirectories(resourcesDir);

        Path privateKeyPath = resourcesDir.resolve("privateKey.pem");
        Path publicKeyPath = resourcesDir.resolve("publicKey.pem");

        if (Files.exists(privateKeyPath) && Files.exists(publicKeyPath)) {
            return;
        }

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        String privateKeyPem = "-----BEGIN RSA PRIVATE KEY-----\n" +
            Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(privateKey.getEncoded()) +
            "\n-----END RSA PRIVATE KEY-----\n";

        String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" +
            Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(publicKey.getEncoded()) +
            "\n-----END PUBLIC KEY-----\n";

        Files.writeString(privateKeyPath, privateKeyPem);
        Files.writeString(publicKeyPath, publicKeyPem);
    }
}
