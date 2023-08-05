package com.tmung.miniminder;

import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.crypto.params.DHPrivateKeyParameters;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DHPrivateKeySpec;
import javax.crypto.spec.DHPublicKeySpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class ParentsKeyExchange {
    private KeyPair keyPair; // Add this line
    private PublicKey childPublicKey; // Child's public key received from the child's app

    public KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
            keyPairGenerator.initialize(2048); // Adjust the key size as needed
            keyPair = keyPairGenerator.generateKeyPair();

            PublicKey publicKey = keyPair.getPublic();
            // Send the public key to the child's app
            String publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            // ... send publicKeyString to the child's app ...

            return keyPair;
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception as needed
            return null;
        }
    }

    public SecretKey deriveAESKey(byte[] sharedSecret, byte[] salt, int keyLength) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(Base64.getEncoder().encodeToString(sharedSecret).toCharArray(), salt, 65536, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = skf.generateSecret(spec).getEncoded();
        SecretKey key = new SecretKeySpec(keyBytes, "AES");
        return key;
    }

    // Method to get the private key parameters
    public DHPrivateKeyParameters getPrivateKeyParams() {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("DH");
            DHPrivateKeySpec privateKeySpec = keyFactory.getKeySpec(keyPair.getPrivate(), DHPrivateKeySpec.class);
            DHPublicKeySpec publicKeySpec = keyFactory.getKeySpec(keyPair.getPublic(), DHPublicKeySpec.class);
            DHParameters dhParams = new DHParameters(publicKeySpec.getP(), publicKeySpec.getG());

            BigInteger privateKeyX = privateKeySpec.getX();
            return new DHPrivateKeyParameters(privateKeyX, dhParams);
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception as needed
            return null;
        }
    }



    public void setChildPublicKey(String publicKeyString) {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString);
            childPublicKey = KeyFactory.getInstance("DH").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception as needed
        }
    }

    public byte[] calculateSharedSecret() {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(keyPair.getPrivate());
            keyAgreement.doPhase(childPublicKey, true);
            return keyAgreement.generateSecret();
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception as needed
            return null;
        }
    }

    // Use the shared secret to derive the encryption key (e.g., AES key) for secure communication
    // ... derive the encryption key ...
}
