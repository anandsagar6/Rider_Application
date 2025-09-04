package com.example.rider;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WaitForDriver_Activity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String rideId, driverId, customerId;

    // Cached values for passing to next screen
    private String driverName = "";
    private String vehicleType = "";
    private String vehicleNumber = "";
    private String price = "";

    private Marker driverMarker;
    private DatabaseReference rideRef, driverRef, customerRef;
    private CountDownTimer countDownTimer;

    private TextView statusText, driverInfo, distanceText, pickupText, destText, timerText, pinText;

    private double pickupLat = 0.0, pickupLng = 0.0;
    private String pickupAddress = "", destinationAddress = "";

    private ValueEventListener rideListener, driverLocationListener;
    private boolean isNavigatingBack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wait_for_driver);

        statusText   = findViewById(R.id.statusText);
        driverInfo   = findViewById(R.id.driverInfo);
        distanceText = findViewById(R.id.distanceText);
        pickupText   = findViewById(R.id.pickupText);
        destText     = findViewById(R.id.destText);
        timerText    = findViewById(R.id.timerText);
        pinText      = findViewById(R.id.pinText);

        pinText.setVisibility(TextView.GONE);

        rideId = getIntent().getStringExtra("ride_id");
        if (rideId == null || rideId.isEmpty()) {
            Toast.makeText(this, "Invalid Ride ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Button btnCancelRide = findViewById(R.id.cancelRideBtn);
        btnCancelRide.setOnClickListener(v -> cancelRide());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        rideRef = FirebaseDatabase.getInstance().getReference("Rides").child(rideId);
        customerRef = FirebaseDatabase.getInstance().getReference("Customers");

        listenForRideUpdates();
        startAutoCancelTimer();
    }

    private void updateRideStatus(String status) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        if (rideRef != null) {
            rideRef.child("status").setValue(status);
        }
        if (customerId != null && rideId != null) {
            rootRef.child("Customers").child(customerId)
                    .child("rides").child(rideId)
                    .child("status").setValue(status);
        }
        if (driverId != null && rideId != null) {
            rootRef.child("drivers").child(driverId)
                    .child("rides").child(rideId)
                    .child("status").setValue(status);
        }
    }

    private void cancelRide() {
        rideRef.child("status").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
                updateRideStatus("cancelled_by_customer");
                rideRef.child("cancelledTime").setValue(currentTime);

                rideRef.removeValue().addOnSuccessListener(aVoid -> {
                    Toast.makeText(WaitForDriver_Activity.this,
                            "Ride Cancelled by You ❌",
                            Toast.LENGTH_SHORT).show();
                    goBackToBooking();
                });
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

                // keep price handy
                String p = snapshot.child("price").getValue(String.class);
                if (p != null) price = p;

                statusText.setText("Ride Status: " + status);

                pickupAddress      = snapshot.child("pickupAddress").getValue(String.class);
                destinationAddress = snapshot.child("destinationAddress").getValue(String.class);

                Double pLat = snapshot.child("pickupLat").getValue(Double.class);
                Double pLng = snapshot.child("pickupLng").getValue(Double.class);

                if (pLat != null) pickupLat = pLat;
                if (pLng != null) pickupLng = pLng;

                if (pickupAddress != null)      pickupText.setText("Pickup: " + pickupAddress);
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
                            // If driverName already saved in ride, use it; else fetch from drivers/info
                            String savedDriverName = snapshot.child("driverName").getValue(String.class);
                            if (savedDriverName != null && !savedDriverName.isEmpty()) {
                                driverName = savedDriverName;
                            }
                            showDriverDetails(driverId);
                            listenForDriverLocation(driverId);
                        }

                        fetchCustomerPin();
                        break;

                    case "ongoing":
                        statusText.setText("Ride in progress...");
                        break;


                    case "completed":
                        statusText.setText("Ride completed 🎉");
                        updateRideStatus("completed");
                        Toast.makeText(WaitForDriver_Activity.this, "Trip Completed", Toast.LENGTH_LONG).show();

                        // ✅ Always fetch all needed values from snapshot
                        String dName = snapshot.child("driverName").getValue(String.class);
                        if (dName != null && !dName.isEmpty()) driverName = dName;

                        String vType  = snapshot.child("vehicleType").getValue(String.class);
                        String vNum   = snapshot.child("vehicleNumber").getValue(String.class);
                        String pAddr  = snapshot.child("pickupName").getValue(String.class);
                        String dAddr  = snapshot.child("Drop").getValue(String.class);
                        String fare   = snapshot.child("price").getValue(String.class);

                        // ✅ Fallbacks if null
                        if (vType != null) vehicleType = vType;
                        if (vNum != null) vehicleNumber = vNum;
                        if (pAddr != null) pickupAddress = pAddr;
                        if (dAddr != null) destinationAddress = dAddr;
                        if (fare != null) price = fare;

                        // ✅ Launch summary screen with guaranteed values
                        Intent intent = new Intent(WaitForDriver_Activity.this, AfterRideComplete_Activity.class);
                        intent.putExtra("rideId", rideId);
                        intent.putExtra("driverName", driverName);
                        intent.putExtra("vehicle", (vehicleType.isEmpty() ? "" : vehicleType) +
                                (vehicleNumber.isEmpty() ? "" : " (" + vehicleNumber + ")"));
                        intent.putExtra("pickup", pickupAddress);
                        intent.putExtra("drop", destinationAddress);
                        intent.putExtra("price", price);
                        startActivity(intent);
                        finish(); // finish this activity once we navigate
                        break;

                    case "cancelled":
                    case "cancelled_by_driver":
                    case "cancelled_by_customer":
                    case "cancelled_by_system":
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

    /**
     * Fetch and show driver info, and cache driverName/vehicle fields.
     * Also updates driverName into Rides/Customers/drivers ride nodes (no overwrite).
     */
    private void showDriverDetails(String driverId) {
        DatabaseReference driverInfoRef = FirebaseDatabase.getInstance()
                .getReference("drivers").child(driverId).child("info");

        driverInfoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    driverName    = safeGet(snapshot, "fullName");
                    String phone  = safeGet(snapshot, "phone");
                    vehicleType   = safeGet(snapshot, "vehicleType");
                    vehicleNumber = safeGet(snapshot, "vehicleNumber");

                    driverInfo.setText("Driver: " + nz(driverName) +
                            "\nPhone: " + nz(phone) +
                            "\nVehicle: " + nz(vehicleType) + (vehicleNumber == null || vehicleNumber.isEmpty() ? "" : " (" + vehicleNumber + ")"));

                    // Update only driverName in all places (don’t overwrite whole ride)
                    Map<String, Object> updateMap = new HashMap<>();
                    updateMap.put("driverName", driverName);

                    FirebaseDatabase db = FirebaseDatabase.getInstance();
                    db.getReference("Rides").child(rideId).updateChildren(updateMap);
                    db.getReference("Customers").child(customerId).child("rides").child(rideId).updateChildren(updateMap);
                    db.getReference("drivers").child(driverId).child("rides").child(rideId).updateChildren(updateMap);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("RideCreation", "Error fetching driver info: " + error.getMessage());
            }
        });
    }

    private String safeGet(DataSnapshot parent, String key) {
        String v = parent.child(key).getValue(String.class);
        return v == null ? "" : v;
    }

    private String nz(String v) { return v == null ? "Not Available" : v; }

    private void listenForDriverLocation(String driverId) {
        driverRef = FirebaseDatabase.getInstance().getReference("drivers").child(driverId);

        driverLocationListener = driverRef.addValueEventListener(new ValueEventListener() {
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
                            float kmAway = results[0] / 1000f;
                            distanceText.setText("Driver is " + String.format(Locale.getDefault(), "%.2f", kmAway) + " km away");
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
        if (isNavigatingBack) return;
        isNavigatingBack = true;

        Intent intent = new Intent(WaitForDriver_Activity.this, DashBoard.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("pickupAddress", pickupAddress);
        intent.putExtra("destinationAddress", destinationAddress);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
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
        if (driverRef != null && driverLocationListener != null)
            driverRef.removeEventListener(driverLocationListener);
        if (countDownTimer != null) countDownTimer.cancel();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
