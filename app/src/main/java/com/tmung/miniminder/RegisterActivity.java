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

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private EditText edtTxtEmail, edtTxtPass;  // Assuming you have these in your layout

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);  // Using the correct layout file

        firebaseAuth = FirebaseAuth.getInstance();

        edtTxtEmail = findViewById(R.id.edtTxtEmail);
        edtTxtPass = findViewById(R.id.edtTxtPass);
    }

    public void register(View view) {  // This method can be directly linked to a button's onClick in XML
        String email = edtTxtEmail.getText().toString().trim();
        String password = edtTxtPass.getText().toString().trim();

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign up success
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
}
