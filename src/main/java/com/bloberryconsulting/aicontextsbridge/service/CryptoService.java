package com.bloberryconsulting.aicontextsbridge.service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;

@Service
public class CryptoService {

    private static final String ALGORITHM = "AES";
    private static final int KEY_LENGTH = 128; // Can be 128, 192, or 256 bits

    @Value("${myapp.secret-key}")
    private String secretKey;

    // Optionally, use a salt if deriving key from a passphrase
    // private static final byte[] SALT = "chooseYourSalt".getBytes(StandardCharsets.UTF_8);

    private SecretKeySpec getKeySpec() throws Exception {
        // Derive the key using PBKDF2
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), new byte[16], 65536, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
    }

    public String encrypt(String valueToEnc) throws Exception {
        SecretKeySpec key = getKeySpec();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encryptedValue = cipher.doFinal(valueToEnc.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeBase64String(encryptedValue);
    }

    public String decrypt(String encryptedValue) throws Exception {
        SecretKeySpec key = getKeySpec();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decodedValue = Base64.decodeBase64(encryptedValue);
        byte[] decryptedValue = cipher.doFinal(decodedValue);
        return new String(decryptedValue, StandardCharsets.UTF_8);
    }
}
