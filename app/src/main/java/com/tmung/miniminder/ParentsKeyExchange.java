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
import java.util.logging.Level;
import java.util.logging.Logger;

// class for handling key exchange between parent and child apps
public class ParentsKeyExchange {
    // class-level variables
    private KeyPair keyPair;
    // Child's public key received from the child's app
    private PublicKey childPublicKey;

    // method to generate key pair for the parent
    public KeyPair generateKeyPair() {
        try {
            // initialise KeyPairGenerator with Diffie-Hellman algorithm
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
            // set key size to 2048 bits
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair(); // generate key pair

            // get the public key to be sent to Firebase
            PublicKey publicKey = keyPair.getPublic();
            // will send the public key string to the child's app
            String publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());

            return keyPair;
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception as needed
            return null;
        }
    }

    // method to derive AES key using PBKDF2 and HMAC-SHA256
    public SecretKey deriveAESKey(byte[] sharedSecret, byte[] salt, int keyLength) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(Base64.getEncoder().encodeToString(sharedSecret).toCharArray(), salt, 65536, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = skf.generateSecret(spec).getEncoded();
        SecretKey key = new SecretKeySpec(keyBytes, "AES");
        return key;
    }

    // method to set the child's public key received from Firebase
    public void setChildPublicKey(String publicKeyString) {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString);
            childPublicKey = KeyFactory.getInstance("DH").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception as needed
        }
    }

    // method to calculate the shared secret using private key and received public key
    public byte[] calculateSharedSecret() {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(keyPair.getPrivate());
            keyAgreement.doPhase(childPublicKey, true);
            return keyAgreement.generateSecret();
        } catch (Exception e) {
            e.printStackTrace();
            // handle exception
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error calculating shared secret", e);
            return null;
        }
    }

}
