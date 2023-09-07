package com.tmung.miniminder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    // declare class-level variables
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private EditText edtTxtEmail, edtTxtPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // initialise Firebase Authenticate
        firebaseAuth = FirebaseAuth.getInstance();
        // get reference to path 'users' in Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        edtTxtEmail = findViewById(R.id.edtTxtEmail); // find EditText containing email
        edtTxtPass = findViewById(R.id.edtTxtPass); // find EditText containing pass
    }

    // method to take user to login page
    public void goToLogin(View view) {
        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
    }

    // method to register an account for user
    public void register(View view) {
        String email = edtTxtEmail.getText().toString().trim();
        String password = edtTxtPass.getText().toString().trim();

        // create new account with user's details
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                // add listener for success
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // after successful registration, set up user in the database
                            setupUserInDatabase();

                            // Sign up success; take user to main app
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            finish();
                        } else {
                            // If sign up fails, display a message to the user.
                            String errorMessage = task.getException().getMessage();
                            Toast.makeText(getApplicationContext(), "Authentication failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // method to set user up in database, so we can keep track of account links
    private void setupUserInDatabase() {
        String userId = firebaseAuth.getCurrentUser().getUid();
        String email = edtTxtEmail.getText().toString().trim();

        // database references setting the values to user's details
        databaseReference.child(userId).child("role").setValue("parent");
        databaseReference.child(userId).child("email").setValue(email); 
        databaseReference.child(userId).child("linkedAccounts").setValue(new HashMap<>());
    }

}
