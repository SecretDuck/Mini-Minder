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
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    // declare class-level variables
    private FirebaseAuth firebaseAuth;
    private EditText edtTxtEmail, edtTxtPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // initialise Firebase Authenticate
        firebaseAuth = FirebaseAuth.getInstance();

        edtTxtEmail = findViewById(R.id.edtTxtEmail); // find EditText holding email
        edtTxtPass = findViewById(R.id.edtTxtPass); // find EditText holding password

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // user is signed in, so skip this login page
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
        // if no user is signed in, the login screen will be shown as usual
    }
    // method to take user to registration page
    public void goToRegister(View view) {
        startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
    }
    // method to send user password reset email
    public void forgotPass(View view) {
        Toast.makeText(this, "Password reset sent to registered email", Toast.LENGTH_LONG).show();
    }

    // method to log user in
    public void login(View view) {
        String email = edtTxtEmail.getText().toString().trim();
        String password = edtTxtPass.getText().toString().trim();

        // use built-in Firebase method to login
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // login successful
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            // if login fails, display a message to the user.
                            String errorMessage = task.getException().getMessage();
                            Toast.makeText(getApplicationContext(), "Authentication failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
