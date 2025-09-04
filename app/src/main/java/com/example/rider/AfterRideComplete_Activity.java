package com.example.rider;

import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AfterRideComplete_Activity extends AppCompatActivity {

    private TextView tvRideId, tvDriverName, tvVehicle, tvPickup, tvDrop, tvFare;

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

        // ✅ Get data from Intent
        String rideId = getIntent().getStringExtra("rideId");
        String driverName = getIntent().getStringExtra("driverName");
        String vehicle = getIntent().getStringExtra("vehicle");
        String pickup = getIntent().getStringExtra("pickup");
        String drop = getIntent().getStringExtra("drop");
        String price = getIntent().getStringExtra("price");

        // ✅ Set values into UI
        tvRideId.setText("Ride ID: " + rideId);
        tvDriverName.setText("Driver: " + driverName);
        tvVehicle.setText("Vehicle: " + vehicle);
        tvPickup.setText("Pickup: " + pickup);
        tvDrop.setText("Drop: " + drop);
        tvFare.setText("Fare: ₹" + price);
    }
}
