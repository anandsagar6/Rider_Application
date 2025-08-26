package com.example.rider;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                // already logged in → go to MainActivity
                startActivity(new Intent(SplashActivity.this, DashBoard.class));
                finish();
            } else {
                // not logged in → go to LoginActivity
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }
            finish();
        }, SPLASH_TIME);
    }
}
