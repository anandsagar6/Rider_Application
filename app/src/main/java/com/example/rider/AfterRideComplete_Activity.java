package com.example.rider;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AfterRideComplete_Activity extends AppCompatActivity {

    private TextView tvRideId, tvDriverName, tvVehicle, tvPickup, tvDrop, tvFare;
    private RatingBar ratingBar;
    private Button btnSubmitRating;
    private ProgressBar progressBar;

    private DatabaseReference ridesRef, driverRideRef;
    private String rideId, driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_after_ride_complete);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ✅ Initialize views
        tvRideId = findViewById(R.id.tvRideId);
        tvDriverName = findViewById(R.id.tvDriverName);
        tvVehicle = findViewById(R.id.tvVehicle);
        tvPickup = findViewById(R.id.tvPickup);
        tvDrop = findViewById(R.id.tvDrop);
        tvFare = findViewById(R.id.tvFare);
        ratingBar = findViewById(R.id.ratingBar);
        btnSubmitRating = findViewById(R.id.btnSubmitRating);
        progressBar = findViewById(R.id.progressBar);

        // ✅ Get rideId from Intent
        rideId = getIntent().getStringExtra("rideId");

        if (rideId == null) {
            Toast.makeText(this, "Ride ID is missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ✅ Firebase reference to global Rides node
        ridesRef = FirebaseDatabase.getInstance().getReference("Rides").child(rideId);

        // ✅ Fetch ride details
        ridesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(AfterRideComplete_Activity.this, "Ride not found!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Fetch details
                driverId = snapshot.child("driverId").getValue(String.class);
                String driverName = snapshot.child("driverName").getValue(String.class);
                String vehicle = snapshot.child("vehicle").getValue(String.class);
                String pickup = snapshot.child("pickup").getValue(String.class);
                String drop = snapshot.child("drop").getValue(String.class);
                String price = snapshot.child("price").getValue(String.class);

                // ✅ Set values in UI
                tvRideId.setText("Ride ID: " + rideId);
                tvDriverName.setText("Driver: " + (driverName != null ? driverName : "N/A"));
                tvVehicle.setText("Vehicle: " + (vehicle != null ? vehicle : "N/A"));
                tvPickup.setText("Pickup: " + (pickup != null ? pickup : "N/A"));
                tvDrop.setText("Drop: " + (drop != null ? drop : "N/A"));
                tvFare.setText("Fare: ₹" + (price != null ? price : "0"));

                if (driverId != null) {
                    driverRideRef = FirebaseDatabase.getInstance().getReference("drivers")
                            .child(driverId)
                            .child("rides")
                            .child(rideId);

                    // ✅ Handle button click
                    btnSubmitRating.setOnClickListener(v -> {
                        int rating = (int) ratingBar.getRating();

                        if (rating == 0) {
                            Toast.makeText(AfterRideComplete_Activity.this, "Please select a rating", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Show progress
                        progressBar.setVisibility(View.VISIBLE);
                        btnSubmitRating.setEnabled(false);

                        driverRideRef.child("rating").setValue(rating)
                                .addOnSuccessListener(unused -> {
                                    progressBar.setVisibility(View.GONE);
                                    btnSubmitRating.setEnabled(true);
                                    Toast.makeText(AfterRideComplete_Activity.this, "You rated " + rating + " stars", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    btnSubmitRating.setEnabled(true);
                                    Toast.makeText(AfterRideComplete_Activity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    });

                } else {
                    Toast.makeText(AfterRideComplete_Activity.this, "Driver ID missing in ride data!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AfterRideComplete_Activity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
