package com.tmung.miniminder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

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

    private SharedViewModel sharedViewModel;
    private ParentsKeyExchange keyExchange;
    private DrawerLayout drawer;

    // SEND SALT TO CHILD'S APP AS WELL
    byte[] salt = new byte[16]; // Define a byte array for the salt

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);

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
        DatabaseReference childPublicKeyRef = FirebaseDatabase.getInstance().getReference("childPublicKey");
        childPublicKeyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String childPublicKeyString = snapshot.getValue(String.class);
                    try {
                        // Set child's public key
                        keyExchange.setChildPublicKey(childPublicKeyString);

                        // Now perform key agreement to obtain the shared secret
                        byte[] sharedSecret = keyExchange.calculateSharedSecret();

                        // Use sharedSecret to derive an AES key
                        SecretKey aesKey = keyExchange.deriveAESKey(sharedSecret, salt, 256); // Use 256-bit key for AES
                        // Pass it to the ViewModel
                        sharedViewModel.setAesKey(aesKey);

                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        e.printStackTrace();
                        // Handle the exception as needed
                    }
                }
            }

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