package com.tmung.miniminder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.crypto.params.DHPublicKeyParameters;
import org.bouncycastle.crypto.params.DHPrivateKeyParameters;
import org.bouncycastle.util.test.FixedSecureRandom;

import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPrivateKeySpec;
import javax.crypto.spec.DHPublicKeySpec;
import org.bouncycastle.crypto.params.DHParameters;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ParentsKeyExchange keyExchange;
    private DatabaseReference childPublicKeyRef;
    private DrawerLayout drawer;

    // SEND SALT TO CHILD'S APP AS WELL
    byte[] salt = new byte[16]; // Define a byte array for the salt

    // Define the domain parameters for the Diffie-Hellman key exchange
    /*private BigInteger p = new BigInteger("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
            + "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
            + "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C24"
            + "416A40BDF122D0A987DBE601D08FFD8A02FAC905D8AEBBD"
            + "E2355DABF599FC6573D89DDF1AC16DFA2CBA3A3BA1863C"
            + "85C97FFFFFFFFFFFFFFFF", 16);

    private BigInteger g = BigInteger.valueOf(2);

    private DHParameterSpec dhParamSpec = new DHParameterSpec(p, g);
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new HomeFragment()).commit();
            navigationView.setCheckedItem(R.id.nav_home);
        }
        // Initialise FirebaseApp
        FirebaseApp.initializeApp(this);

        // Generate a random salt
        new SecureRandom().nextBytes(salt);

        // Diffie-Hellman key exchange to encrypt data
        keyExchange = new ParentsKeyExchange();
        KeyPair keyPair = keyExchange.generateKeyPair(); // Generate the public/private key pair
        String parentsPublicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        // Send parent's public key to Firebase, so the child's app can derive the shared secret
        DatabaseReference parentPublicKey = FirebaseDatabase.getInstance().getReference("parentPublicKey");
        parentPublicKey.setValue(parentsPublicKey);

        // After the child's public key is obtained from Firebase
        childPublicKeyRef = FirebaseDatabase.getInstance().getReference("childPublicKey");
        childPublicKeyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String childPublicKeyString = dataSnapshot.getValue(String.class);
                    try {
                        // Set child's public key
                        keyExchange.setChildPublicKey(childPublicKeyString);

                        // Now perform key agreement to obtain the shared secret
                        byte[] sharedSecret = keyExchange.calculateSharedSecret();

                        // Use sharedSecret to derive an AES key
                        SecretKey aesKey = keyExchange.deriveAESKey(sharedSecret, salt, 256); // Use 256-bit key for AES
                        // Use aesKey for encryption and decryption
                        // ...

                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        e.printStackTrace();
                        // Handle the exception as needed
                    }
                }
            }

            /*
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String childPublicKeyString = dataSnapshot.getValue(String.class);
                    byte[] childPublicKeyBytes = Base64.getDecoder().decode(childPublicKeyString);

                    try {
                        DHParameterSpec dhParamSpec = new DHParameterSpec(p, g);
                        DHParameters dhParams = new DHParameters(dhParamSpec.getP(), dhParamSpec.getG());

                        // Decode the child's public key and create DHPublicKeySpec
                        BigInteger childPublicKeyInt = new BigInteger(1, childPublicKeyBytes);
                        DHPublicKeySpec publicKeySpec = new DHPublicKeySpec(childPublicKeyInt, dhParamSpec.getP(), dhParamSpec.getG());
                        DHPublicKeyParameters childPublicKeyParams = new DHPublicKeyParameters(publicKeySpec.getY(), dhParams);

                        // Now perform key agreement to obtain the shared secret
                        DHPrivateKeyParameters privateKeyParams = keyExchange.getPrivateKeyParams();
                        KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");

                        // Convert DHPrivateKeyParameters to PrivateKey
                        KeyFactory keyFactory = KeyFactory.getInstance("DH");
                        DHPrivateKeySpec dhPrivateKeySpec = new DHPrivateKeySpec(privateKeyParams.getX(), dhParams.getP(), dhParams.getG());
                        PrivateKey privateKey = keyFactory.generatePrivate(dhPrivateKeySpec);

                        keyAgreement.init(privateKey);

                        // Convert DHPublicKeyParameters to PublicKey
                        DHPublicKeySpec dhPublicKeySpec = new DHPublicKeySpec(childPublicKeyInt, dhParams.getP(), dhParams.getG());
                        PublicKey childPublicKey = keyFactory.generatePublic(dhPublicKeySpec);

                        keyAgreement.doPhase(childPublicKey, true);
                        byte[] sharedSecret = keyAgreement.generateSecret();

                        // Use sharedSecret as the derived encryption key (e.g., AES key) for secure communication
                        // ...
                    } catch (Exception e) {
                        e.printStackTrace();
                        // Handle the exception as needed
                    }
                }
            }
             */

            //@Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle errors if needed
            }
        });



    } // End onCreate here

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new HomeFragment()).commit();
        } else if (id == R.id.nav_map) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new SimulationFragment()).commit();
        } else if (id == R.id.nav_child) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new ChildLocFragment()).commit();
        }

        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}