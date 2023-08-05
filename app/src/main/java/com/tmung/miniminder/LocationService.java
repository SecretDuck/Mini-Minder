package com.tmung.miniminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class LocationService extends Service {
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;

    private final String CHANNEL_ID = "123"; // define your own channel ID
    private final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // Get the best most recent location.
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    // Save location data to Firebase.
                    DatabaseReference childLocations = FirebaseDatabase.getInstance().getReference("childLocations");
                    childLocations.setValue(new LocationData(
                            location.getLatitude(), location.getLongitude()));
                }
            }
        };

        createNotificationChannel();

        // Generate key for encryption and decryption
        //generateEncryptionKey();
    } // End of onCreate
/*
    private void generateEncryptionKey() {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder("MyKeyAlias",
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT);
        builder.setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);
        keyGenerator.init(builder.build());
        SecretKey key = keyGenerator.generateKey();
    }
    private String encryptData(String plaintext) {
        // To encrypt
        Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal("Hello, world!".getBytes(StandardCharsets.UTF_8));

        // Convert encrypted byte array to string
        return Base64.encodeToString(encrypted, Base64.DEFAULT);
    }
    private String decryptData(String encryptedData) {
        // Convert encrypted string to byte array
        byte[] encrypted = Base64.decode(encryptedData, Base64.DEFAULT);

        // To decrypt
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(cipher.getIV()));
        byte[] decrypted = cipher.doFinal(encrypted);
        String originalString = new String(decrypted, StandardCharsets.UTF_8);

        // Return the decrypted data
        return originalString;
    }
    private void generateEncryptionKey() {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder("MyKeyAlias",
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT);
        builder.setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);
        keyGenerator.init(builder.build());
        SecretKey key = keyGenerator.generateKey();
    }

 */

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // provide your own icon
                .setContentTitle("Location Service")
                .setContentText("Tracking child location...")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        startForeground(NOTIFICATION_ID, builder.build());

        startLocationUpdates();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startLocationUpdates() {
        fusedLocationProviderClient.requestLocationUpdates(
                LocationRequest.create().setInterval(1000).setFastestInterval(500).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY),
                locationCallback,
                Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void createNotificationChannel() {
        CharSequence name = "LocationService";
        String description = "Used for getting child's location";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }
}