package com.example.rider;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SignupActivity extends AppCompatActivity {

    private EditText edtName, edtPhone, edtEmail, edtPassword;
    private Button btnSignup;
    private TextView btnGotoLogin,privacy,term;
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        edtName = findViewById(R.id.edtName);
        edtPhone = findViewById(R.id.edtPhone);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnSignup = findViewById(R.id.btnSignup);
        btnGotoLogin = findViewById(R.id.btnGotoLogin);
        progressBar = findViewById(R.id.progressBarSignup);
        privacy=findViewById(R.id.txtPrivacyPolicy);
        term=findViewById(R.id.txtTermsConditions);


        privacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(SignupActivity.this,PrivacyPolicyActivity.class);
                startActivity(i);
            }
        });
        term.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(SignupActivity.this,TermsConditionsActivity.class);
                startActivity(i);
            }
        });


        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("Customers");

        btnSignup.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            boolean isValid = true;

            if (name.isEmpty()) {
                edtName.setError("Name is required");
                isValid = false;
            }
            if (phone.isEmpty()) {
                edtPhone.setError("Phone is required");
                isValid = false;
            } else if (!phone.matches("\\d{10}")) {
                edtPhone.setError("Enter a valid 10-digit phone number");
                isValid = false;
            }
            if (email.isEmpty()) {
                edtEmail.setError("Email is required");
                isValid = false;
            }
            if (password.isEmpty()) {
                edtPassword.setError("Password is required");
                isValid = false;
            } else if (password.length() < 6) {
                edtPassword.setError("Password must be at least 6 characters long");
                isValid = false;
            }

            if (!isValid) return;

            progressBar.setVisibility(View.VISIBLE);
            btnSignup.setEnabled(false);

            dbRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    boolean phoneExists = false;
                    for (DataSnapshot snapshot : task.getResult().getChildren()) {
                        String existingPhone = snapshot.child("phone").getValue(String.class);
                        if (phone.equals(existingPhone)) {
                            phoneExists = true;
                            break;
                        }
                    }

                    if (phoneExists) {
                        edtPhone.setError("This phone number is already registered");
                        edtPhone.requestFocus();
                        progressBar.setVisibility(View.GONE);
                        btnSignup.setEnabled(true);
                    } else {
                        createNewUser(name, phone, email, password);
                    }
                } else {
                    edtPhone.setError("Error checking phone number. Try again.");
                    progressBar.setVisibility(View.GONE);
                    btnSignup.setEnabled(true);
                }
            });
        });

        btnGotoLogin.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    private void createNewUser(String name, String phone, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            btnSignup.setEnabled(true);

            if (task.isSuccessful()) {
                String uid = mAuth.getCurrentUser().getUid();

                int pin = 1000 + new Random().nextInt(9000);

                Map<String, Object> customerData = new HashMap<>();
                customerData.put("name", name);
                customerData.put("phone", phone);
                customerData.put("email", email);
                customerData.put("pin", String.valueOf(pin));

                dbRef.child(uid).setValue(customerData).addOnCompleteListener(saveTask -> {
                    if (saveTask.isSuccessful()) {
                        Intent intent = new Intent(SignupActivity.this, DashBoard.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    } else {
                        edtPassword.setError("Error: " + saveTask.getException().getMessage());
                    }
                });
            } else {
                edtEmail.setError("Signup Failed: " + task.getException().getMessage());
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
