package com.tmung.miniminder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
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

    // Class-level variables
    private FirebaseAuth firebaseAuth;
    private SharedViewModel sharedViewModel;
    private ValueEventListener childPublicKeyEventListener;
    private DatabaseReference childPublicKeyRef;
    private ParentsKeyExchange keyExchange;
    private DrawerLayout drawer;

    // Define a byte array for the salt, for key derivation
    byte[] salt = { (byte) 0xa9, (byte) 0x0f, (byte) 0x3b, (byte) 0x7e, (byte) 0xb3, (byte) 0x21, (byte) 0x4a, (byte) 0x6f, (byte) 0x85, (byte) 0xc9, (byte) 0xe0, (byte) 0xf1, (byte) 0x5d, (byte) 0x6c, (byte) 0x7b, (byte) 0x8a };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialise Firebase, and ViewModel below that
        firebaseAuth = FirebaseAuth.getInstance();
        // initialise shared view model
        sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up navigation drawer
        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Add switch for location tracking in the navigation drawer
        FrameLayout switchContainer = (FrameLayout) navigationView.getMenu().findItem(R.id.nav_switch_item_container).getActionView();
        SwitchCompat trackingSwitch = (SwitchCompat) LayoutInflater.from(this).inflate(R.layout.layout_switch, switchContainer, false);
        switchContainer.addView(trackingSwitch);

        // Handle switch changes for tracking
        trackingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Handling switch changes
                trackingSwitch.setText(isChecked ? "On" : "Off");

                // Set the isLocationUpdating flag in the Firebase database to the new state of the switch.
                DatabaseReference isLocationUpdatingRef = FirebaseDatabase.getInstance().getReference("isLocationUpdating");
                isLocationUpdatingRef.setValue(isChecked);
            }
        });

        // ActionBarDrawerToggle to manage drawer open/close actions
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Set default fragment as Home if no instance is saved
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new HomeFragment()).commit();
            navigationView.setCheckedItem(R.id.nav_home);
        }
        // Initialise FirebaseApp
        FirebaseApp.initializeApp(this);

        // Diffie-Hellman key exchange to encrypt data
        keyExchange = new ParentsKeyExchange();
        KeyPair keyPair = keyExchange.generateKeyPair(); // Generate the public/private key pair
        String parentsPublicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        // Send parent's public key to Firebase, so the child's app can derive the shared secret
        DatabaseReference parentPublicKeyRef = FirebaseDatabase.getInstance().getReference("parentPublicKey");
        parentPublicKeyRef.setValue(parentsPublicKey);

        // Listen for child's public key
        childPublicKeyRef = FirebaseDatabase.getInstance().getReference("childPublicKey");
        childPublicKeyEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String childPublicKeyString = snapshot.getValue(String.class);
                    try {
                        // Set child's public key
                        keyExchange.setChildPublicKey(childPublicKeyString);

                        // Perform key agreement to obtain the shared secret
                        byte[] sharedSecret = keyExchange.calculateSharedSecret();

                        // Use sharedSecret to derive an AES key
                        SecretKey aesKey = keyExchange.deriveAESKey(sharedSecret, salt, 256); // Use 256-bit key for AES
                        // Pass it to the ViewModel
                        sharedViewModel.setAesKey(aesKey);

                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // handle error
                Log.e("DatabaseError", "Error: " + databaseError.getMessage());
            }
        };
        // add the event listener
        childPublicKeyRef.addValueEventListener(childPublicKeyEventListener);
    } // End onCreate here

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Switching between different fragments based on navigation selection
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new HomeFragment()).commit();
        } else if (id == R.id.nav_simulation) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new SimulationFragment()).commit();
        } else if (id == R.id.nav_profile) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new ProfileFragment()).commit();
        } else if (id == R.id.nav_logout) {
            // Log user out
            firebaseAuth.signOut();

            // After logging out, navigate back to the LoginActivity
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }
        // Close drawer after option is selected
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    @Override
    public void onBackPressed() {
        // Pressing 'back' closes the navigation drawer
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        // Remove ValueEventListener when the activity is destroyed, to prevent memory leaks
        if (childPublicKeyRef != null && childPublicKeyEventListener != null) {
            childPublicKeyRef.removeEventListener(childPublicKeyEventListener);
        }
        super.onDestroy();
    }
}