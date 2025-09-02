package com.example.rider;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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
    private TextView btnGotoLogin;
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

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
            } else if (!phone.matches("\\d{10}")) {  // ✅ Must be 10 digits
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
            } else if (password.length() < 6) {  // ✅ Minimum 6 characters
                edtPassword.setError("Password must be at least 6 characters long");
                isValid = false;
            }

            if (!isValid) return;

            // ✅ Check if phone already exists in DB
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
                    } else {
                        // ✅ Proceed with Firebase Auth
                        createNewUser(name, phone, email, password);
                    }
                } else {
                    edtPhone.setError("Error checking phone number. Try again.");
                }
            });
        });

        btnGotoLogin.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });
    }

    private void createNewUser(String name, String phone, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = mAuth.getCurrentUser().getUid();

                // ✅ Generate random 4-digit PIN
                int pin = 1000 + new Random().nextInt(9000);

                // Save customer data + pin into Firebase
                Map<String, Object> customerData = new HashMap<>();
                customerData.put("name", name);
                customerData.put("phone", phone);
                customerData.put("email", email);
                customerData.put("pin", String.valueOf(pin));

                dbRef.child(uid).setValue(customerData).addOnCompleteListener(saveTask -> {
                    if (saveTask.isSuccessful()) {
                        edtPassword.setError("Signup Successful! Your PIN: " + pin);
                        edtPassword.requestFocus();

                        startActivity(new Intent(SignupActivity.this, DashBoard.class));
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

    public void onBackPressed() {
        super.onBackPressed();
        // Apply reverse transition when going back
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

}
