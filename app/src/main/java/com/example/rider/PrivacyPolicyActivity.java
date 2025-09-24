package com.example.rider;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class PrivacyPolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        // Optional: Set title
        setTitle("Privacy Policy");


        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

    }
}
