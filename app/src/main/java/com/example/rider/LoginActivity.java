package com.example.rider;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnLogin;
    private TextView btnGotoSignup;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGotoSignup = findViewById(R.id.btnGotoSignup);
        progressBar = findViewById(R.id.progressBar);

        // 🔹 LOGIN button
        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (email.isEmpty()) {
                edtEmail.setError("Email is required");
                edtEmail.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtEmail.setError("Enter a valid email");
                edtEmail.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                edtPassword.setError("Password is required");
                edtPassword.requestFocus();
                return;
            }

            if (password.length() < 6) {
                edtPassword.setError("Password must be at least 6 characters");
                edtPassword.requestFocus();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);

            // 🔹 Try login
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(loginTask -> {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);

                        if (loginTask.isSuccessful()) {
                            startActivity(new Intent(LoginActivity.this, DashBoard.class));
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                            finish();
                        } else {
                            Exception e = loginTask.getException();

                            if (e instanceof FirebaseAuthException) {
                                String errorCode = ((FirebaseAuthException) e).getErrorCode();

                                switch (errorCode) {
                                    case "ERROR_USER_NOT_FOUND":
                                        edtEmail.setError("No account found with this email");
                                        edtEmail.requestFocus();
                                        break;

                                    case "ERROR_WRONG_PASSWORD":
                                        edtPassword.setError("Incorrect password");
                                        edtPassword.requestFocus();
                                        break;

                                    case "ERROR_INVALID_EMAIL":
                                        edtEmail.setError("Invalid email format");
                                        edtEmail.requestFocus();
                                        break;

                                    default:
                                        Toast.makeText(this, "Login Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        break;
                                }
                            } else {
                                // Handle other Firebase errors safely
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });

        // 🔹 CREATE ACCOUNT button → go to SignupActivity
        btnGotoSignup.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        });
    }
}
