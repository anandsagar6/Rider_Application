package com.example.rider;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SignupActivity extends AppCompatActivity {

    private EditText edtName, edtPhone, edtEmail, edtPassword;
    private Button btnSignup;
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

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("Customers");

        btnSignup.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

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
                    customerData.put("pin", String.valueOf(pin)); // store PIN as String

                    dbRef.child(uid).setValue(customerData).addOnCompleteListener(saveTask -> {
                        if (saveTask.isSuccessful()) {
                            Toast.makeText(this, "Signup Successful. Your PIN: " + pin, Toast.LENGTH_LONG).show();

                            // Go to dashboard
                            startActivity(new Intent(SignupActivity.this, DashBoard.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Error saving user: " + saveTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(this, "Signup Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
