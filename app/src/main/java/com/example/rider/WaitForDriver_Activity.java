package com.example.rider;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class WaitForDriver_Activity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String rideId, driverId, customerId;
    private Marker driverMarker;
    private DatabaseReference rideRef, driverRef, customerRef;
    private CountDownTimer countDownTimer;

    private TextView statusText, driverInfo, distanceText, pickupText, destText, timerText, pinText;

    private double pickupLat = 0.0, pickupLng = 0.0;

    private ValueEventListener rideListener, driverLocationListener;

    private String pickupAddress = "", destinationAddress = "";

    private boolean isNavigatingBack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wait_for_driver);

        statusText = findViewById(R.id.statusText);
        driverInfo = findViewById(R.id.driverInfo);
        distanceText = findViewById(R.id.distanceText);
        pickupText = findViewById(R.id.pickupText);
        destText = findViewById(R.id.destText);
        timerText = findViewById(R.id.timerText);
        pinText = findViewById(R.id.pinText);

        // Hide PIN initially
        pinText.setVisibility(TextView.GONE);

        // Get rideId from Address_Activity
        rideId = getIntent().getStringExtra("ride_id");

        if (rideId == null || rideId.isEmpty()) {
            Toast.makeText(this, "Invalid Ride ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Get logged-in customerId
        customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Button btnCancelRide = findViewById(R.id.cancelRideBtn);
        btnCancelRide.setOnClickListener(v -> cancelRide());

        // Setup Google Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // References
        rideRef = FirebaseDatabase.getInstance().getReference("Rides").child(rideId);
        customerRef = FirebaseDatabase.getInstance().getReference("Customers");

        // Listen for ride updates
        listenForRideUpdates();

        // Start 1 min auto cancel timer
        startAutoCancelTimer();
    }

    /**
     * ✅ Helper method to update ride status in both:
     * 1. Rides/{rideId}
     * 2. Customers/{customerId}/rides/{rideId}
     */
    private void updateRideStatus(String status) {
        if (rideRef != null) {
            rideRef.child("status").setValue(status);
        }

        if (customerId != null && rideId != null) {
            DatabaseReference customerRideRef = FirebaseDatabase.getInstance()
                    .getReference("Customers")
                    .child(customerId)
                    .child("rides")
                    .child(rideId)
                    .child("status");

            customerRideRef.setValue(status);
        }
    }

    private void cancelRide() {
        rideRef.child("status").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.getValue(String.class);

                // format current time like "hh:mm a"
                String currentTime = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                        .format(new java.util.Date());

                if ("waiting".equals(status)) {
                    // No driver yet → mark and delete
                    updateRideStatus("cancelled_by_customer");
                    rideRef.child("cancelledTime").setValue(currentTime);

                    rideRef.removeValue().addOnSuccessListener(aVoid -> {
                        Toast.makeText(WaitForDriver_Activity.this,
                                "Ride Cancelled by You ❌",
                                Toast.LENGTH_SHORT).show();
                        goBackToBooking();
                    });

                } else {
                    // Driver already accepted or ride ongoing → just mark cancelled
                    updateRideStatus("cancelled_by_customer");
                    rideRef.child("cancelledTime").setValue(currentTime)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(WaitForDriver_Activity.this,
                                        "Ride Cancelled by You ❌",
                                        Toast.LENGTH_SHORT).show();
                                goBackToBooking();
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenForRideUpdates() {
        rideListener = rideRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    statusText.setText("Ride Cancelled ❌");
                    goBackToBooking();
                    return;
                }

                String status = snapshot.child("status").getValue(String.class);
                if (status == null) status = "waiting";

                statusText.setText("Ride Status: " + status);

                pickupAddress = snapshot.child("pickupAddress").getValue(String.class);
                destinationAddress = snapshot.child("destinationAddress").getValue(String.class);

                Double pLat = snapshot.child("pickupLat").getValue(Double.class);
                Double pLng = snapshot.child("pickupLng").getValue(Double.class);

                if (pLat != null) pickupLat = pLat;
                if (pLng != null) pickupLng = pLng;

                if (pickupAddress != null) pickupText.setText("Pickup: " + pickupAddress);
                if (destinationAddress != null) destText.setText("Destination: " + destinationAddress);

                switch (status) {
                    case "waiting":
                        statusText.setText("Looking for drivers...");
                        pinText.setVisibility(TextView.GONE);
                        break;

                    case "accepted":
                        statusText.setText("Driver is on the way!");
                        driverId = snapshot.child("driverId").getValue(String.class);

                        if (countDownTimer != null) {
                            countDownTimer.cancel();
                            timerText.setText("");
                        }

                        if (driverId != null) {
                            showDriverDetails(driverId);
                            listenForDriverLocation(driverId);
                        }

                        // ✅ Fetch and show customer PIN
                        fetchCustomerPin();
                        break;

                    case "ongoing":
                        statusText.setText("Ride in progress...");
                        break;

                    case "completed":
                        statusText.setText("Ride completed 🎉");
                        updateRideStatus("completed");
                        Toast.makeText(WaitForDriver_Activity.this, "Trip Completed", Toast.LENGTH_LONG).show();
                        finish();
                        break;

                    case "cancelled":
                        statusText.setText("Ride cancelled ❌");
                        updateRideStatus("cancelled");
                        goBackToBooking();
                        break;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchCustomerPin() {
        customerRef.child(customerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String pin = snapshot.child("pin").getValue(String.class);
                    pinText.setVisibility(TextView.VISIBLE);
                    pinText.setText("PIN: " + pin);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showDriverDetails(String driverId) {
        driverRef = FirebaseDatabase.getInstance().getReference("drivers").child(driverId);
        driverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String vehicle = snapshot.child("vehicle").getValue(String.class);

                    driverInfo.setText("Driver: " + name + "\nPhone: " + phone + "\nVehicle: " + vehicle);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenForDriverLocation(String driverId) {
        DatabaseReference driverLocationRef = FirebaseDatabase.getInstance()
                .getReference("drivers").child(driverId);

        driverLocationListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && mMap != null) {
                    Double lat = snapshot.child("currentLat").getValue(Double.class);
                    Double lng = snapshot.child("currentLng").getValue(Double.class);

                    if (lat != null && lng != null) {
                        LatLng driverPos = new LatLng(lat, lng);

                        if (driverMarker == null) {
                            driverMarker = mMap.addMarker(new MarkerOptions()
                                    .position(driverPos)
                                    .title("Driver")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(driverPos, 15));
                        } else {
                            driverMarker.setPosition(driverPos);
                        }

                        if (pickupLat != 0 && pickupLng != 0) {
                            float[] results = new float[1];
                            Location.distanceBetween(lat, lng, pickupLat, pickupLng, results);
                            float kmAway = results[0] / 1000;
                            distanceText.setText("Driver is " + String.format("%.2f", kmAway) + " km away");
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void startAutoCancelTimer() {
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                timerText.setText("Auto cancel in: " + seconds + "s");
            }

            @Override
            public void onFinish() {
                rideRef.child("status").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String status = snapshot.getValue(String.class);
                        if ("waiting".equals(status)) {
                            updateRideStatus("cancelled_by_system");
                            rideRef.removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        statusText.setText("No driver accepted. Ride deleted ❌");
                                        Toast.makeText(WaitForDriver_Activity.this,
                                                "No driver accepted. Ride cancelled & deleted!",
                                                Toast.LENGTH_LONG).show();
                                        goBackToBooking();
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
        }.start();
    }

    private void goBackToBooking() {
        if (isNavigatingBack) return; // ✅ Already going back, skip
        isNavigatingBack = true;

        Intent intent = new Intent(WaitForDriver_Activity.this, DashBoard.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.putExtra("pickupAddress", pickupAddress);
        intent.putExtra("destinationAddress", destinationAddress);

        startActivity(intent);
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rideListener != null) rideRef.removeEventListener(rideListener);
        if (driverRef != null && driverLocationListener != null) driverRef.removeEventListener(driverLocationListener);
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
