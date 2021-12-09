package com.example.myapplication;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class AESManager {
    public AESManager() {}

    public static String encrypt(String jsonPayload, String key) {
        // decode the base64 encoded string
        byte[] decodedKey = key.getBytes(StandardCharsets.UTF_8);//Base64.getDecoder().decode(key);
        // rebuild key using SecretKeySpec
        SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, originalKey);
            // new String(bytes, UTF8_CHARSET)
            return Base64.getEncoder().encodeToString(cipher.doFinal(jsonPayload.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception e) {
            System.out.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    public static String decrypt(String jsonPayload, String key) {
        // decode the base64 encoded string
        byte[] decodedKey = Base64.getDecoder().decode(key);
        // rebuild key using SecretKeySpec
        //SecretKeySpec originalKey = new SecretKeySpec(decodedKey, "AES");
        SecretKeySpec originalKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");

        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");

            cipher.init(Cipher.DECRYPT_MODE, originalKey);
            //byte [] decodedPayload = Base64.getDecoder().decode(jsonPayload);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(jsonPayload));
            String decryptedStr = new String(decrypted);
            decryptedStr = decryptedStr.substring(decryptedStr.indexOf("{"), decryptedStr.lastIndexOf("}") + 1);
            decryptedStr = decryptedStr.replace("\\\"", "'");
            return decryptedStr;
        }
        catch (Exception e) {
            System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }
}
