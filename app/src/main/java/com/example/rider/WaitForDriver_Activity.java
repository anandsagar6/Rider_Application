package com.example.rider;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class WaitForDriver_Activity extends FragmentActivity {

    private MapView mMap;
    private MyLocationNewOverlay myLocationOverlay;

    private String rideId, driverId, customerId;
    private String driverName = "", vehicleType = "", vehicleNumber = "", price = "";

    private Marker driverMarker, pickupMarker, destMarker;
    private DatabaseReference rideRef, driverRef, customerRef;
    private CountDownTimer countDownTimer;

    private TextView statusText, driverInfo, distanceText, pickupText, destText, timerText, pinText;

    private double pickupLat = 0.0, pickupLng = 0.0;
    private double destLat = 0.0, destLng = 0.0;
    private String pickupAddress = "", destinationAddress = "";

    private Double lastDriverLat = null, lastDriverLng = null;

    private ValueEventListener rideListener, driverLocationListener;
    private boolean isNavigatingBack = false;

    // ✅ Keep separate overlays for routes
    private Polyline driverToPickupRoute;
    private Polyline pickupToDropRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wait_for_driver);

        // OSMDroid config
        Configuration.getInstance().setUserAgentValue(getPackageName());

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

        // ✅ Setup OSMDroid MapView
        mMap = findViewById(R.id.map);
        mMap.setMultiTouchControls(true);
        mMap.getController().setZoom(15.0);
        mMap.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER);

        // ✅ Show user location initially
        showUserLocation();

        rideRef = FirebaseDatabase.getInstance().getReference("Rides").child(rideId);
        customerRef = FirebaseDatabase.getInstance().getReference("Customers");

        listenForRideUpdates();
        startAutoCancelTimer();
    }

    private void showUserLocation() {
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mMap);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        mMap.getOverlays().add(myLocationOverlay);
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
        if (rideRef == null) return;
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
                }).addOnFailureListener(e -> {
                    Toast.makeText(WaitForDriver_Activity.this,
                            "Failed to cancel ride: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenForRideUpdates() {
        if (rideRef == null) return;

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

                String p = snapshot.child("price").getValue(String.class);
                if (p != null) price = p;

                statusText.setText("Ride Status: " + status);

                // ✅ Always fetch pickup & destination from Firebase
                pickupAddress      = snapshot.child("pickupAddress").getValue(String.class);
                destinationAddress = snapshot.child("destinationAddress").getValue(String.class);

                Double pLat = snapshot.child("pickupLat").getValue(Double.class);
                Double pLng = snapshot.child("pickupLng").getValue(Double.class);
                Double dLat = snapshot.child("destLat").getValue(Double.class);
                Double dLng = snapshot.child("destLng").getValue(Double.class);

                if (pLat != null) pickupLat = pLat;
                if (pLng != null) pickupLng = pLng;
                if (dLat != null) destLat = dLat;
                if (dLng != null) destLng = dLng;

                if (pickupAddress != null)      pickupText.setText("Pickup: " + pickupAddress);
                if (destinationAddress != null) destText.setText("Destination: " + destinationAddress);

                // ✅ Show pickup & destination markers
                showPickupAndDestination();

                switch (status) {
                    case "waiting":
                        statusText.setText("Looking for drivers...");
                        pinText.setVisibility(TextView.GONE);

                        // ✅ Show pickup → drop route
                        drawPickupToDropRoute();
                        break;

                    case "accepted":
                        statusText.setText("Driver is on the way!");
                        driverId = snapshot.child("driverId").getValue(String.class);

                        if (countDownTimer != null) {
                            countDownTimer.cancel();
                            timerText.setText("");
                        }

                        if (driverId != null && !driverId.isEmpty()) {
                            showDriverDetails(driverId);
                            listenForDriverLocation(driverId);

                            if (lastDriverLat != null && lastDriverLng != null) {
                                // ✅ Driver → Pickup
                                fetchRoute(lastDriverLat, lastDriverLng, pickupLat, pickupLng, true);
                            }

                            // ✅ Pickup → Drop
                            drawPickupToDropRoute();
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

                        Intent intent = new Intent(WaitForDriver_Activity.this, AfterRideComplete_Activity.class);
                        intent.putExtra("rideId", rideId);
                        intent.putExtra("driverName", driverName);
                        intent.putExtra("vehicle", (vehicleType.isEmpty() ? "" : vehicleType) +
                                (vehicleNumber.isEmpty() ? "" : " (" + vehicleNumber + ")"));
                        intent.putExtra("pickup", pickupAddress);
                        intent.putExtra("drop", destinationAddress);
                        intent.putExtra("price", price);
                        startActivity(intent);
                        finish();
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
        if (customerRef == null || customerId == null) return;
        customerRef.child(customerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String pin = snapshot.child("pin").getValue(String.class);
                    if (pin != null && !pin.isEmpty()) {
                        pinText.setVisibility(TextView.VISIBLE);
                        pinText.setText("PIN: " + pin);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showDriverDetails(String driverId) {
        if (driverId == null || driverId.isEmpty()) return;

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
                            "\nVehicle: " + nz(vehicleType) +
                            (vehicleNumber == null || vehicleNumber.isEmpty() ? "" : " (" + vehicleNumber + ")"));

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

    private String nz(String v) { return (v == null || v.isEmpty()) ? "Not Available" : v; }

    private void listenForDriverLocation(String driverId) {
        if (driverId == null || driverId.isEmpty()) return;

        driverRef = FirebaseDatabase.getInstance().getReference("drivers").child(driverId);

        if (driverLocationListener != null && driverRef != null) {
            try { driverRef.removeEventListener(driverLocationListener); } catch (Exception ignored) {}
        }

        driverLocationListener = driverRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                Double lat = snapshot.child("currentLat").getValue(Double.class);
                Double lng = snapshot.child("currentLng").getValue(Double.class);

                lastDriverLat = lat;
                lastDriverLng = lng;

                if (lat != null && lng != null) {
                    GeoPoint driverPos = new GeoPoint(lat, lng);

                    if (driverMarker == null) {
                        driverMarker = new Marker(mMap);
                        driverMarker.setPosition(driverPos);
                        driverMarker.setTitle("Driver");
                        driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        mMap.getOverlays().add(driverMarker);
                        mMap.getController().setCenter(driverPos);
                    } else {
                        driverMarker.setPosition(driverPos);
                        mMap.invalidate();
                    }

                    if (pickupLat != 0 && pickupLng != 0) {
                        float[] results = new float[1];
                        Location.distanceBetween(lat, lng, pickupLat, pickupLng, results);
                        float kmAway = results[0] / 1000f;
                        distanceText.setText("Driver is " + String.format(Locale.getDefault(), "%.2f", kmAway) + " km away");

                        // ✅ update route Driver → Pickup in real-time
                        fetchRoute(lat, lng, pickupLat, pickupLng, true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("WaitForDriver", "Driver location listener cancelled: " + error.getMessage());
            }
        });
    }

    private void fetchRoute(double startLat, double startLng, double endLat, double endLng) {
        fetchRoute(startLat, startLng, endLat, endLng, false);
    }

    private void fetchRoute(double startLat, double startLng, double endLat, double endLng, boolean isDriverRoute) {
        String url = "https://router.project-osrm.org/route/v1/driving/"
                + startLng + "," + startLat + ";" + endLng + "," + endLat
                + "?overview=full&geometries=geojson";

        new Thread(() -> {
            try {
                URL u = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setRequestMethod("GET");
                InputStream in = conn.getInputStream();
                Scanner s = new Scanner(in).useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                in.close();

                JSONObject json = new JSONObject(response);
                JSONArray coords = json.getJSONArray("routes")
                        .getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates");

                ArrayList<GeoPoint> geoPoints = new ArrayList<>();
                for (int i = 0; i < coords.length(); i++) {
                    double lon = coords.getJSONArray(i).getDouble(0);
                    double lat = coords.getJSONArray(i).getDouble(1);
                    geoPoints.add(new GeoPoint(lat, lon));
                }

                runOnUiThread(() -> {
                    Polyline line = new Polyline();
                    line.setPoints(geoPoints);
                    line.setWidth(8f);
                    line.setColor(isDriverRoute ? Color.RED : Color.BLUE);

                    if (isDriverRoute) {
                        if (driverToPickupRoute != null) mMap.getOverlays().remove(driverToPickupRoute);
                        driverToPickupRoute = line;
                    } else {
                        if (pickupToDropRoute != null) mMap.getOverlays().remove(pickupToDropRoute);
                        pickupToDropRoute = line;
                    }

                    mMap.getOverlays().add(line);
                    mMap.invalidate();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void drawPickupToDropRoute() {
        if (pickupLat != 0 && pickupLng != 0 && destLat != 0 && destLng != 0) {
            fetchRoute(pickupLat, pickupLng, destLat, destLng, false);
        }
    }

    private void startAutoCancelTimer() {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                timerText.setText("Auto cancel in: " + seconds + "s");
            }

            @Override
            public void onFinish() {
                if (rideRef == null) return;
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
                                    }).addOnFailureListener(e -> Log.e("WaitForDriver", "Failed to remove ride: " + e.getMessage()));
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

        if (rideListener != null && rideRef != null) rideRef.removeEventListener(rideListener);
        if (driverLocationListener != null && driverRef != null) driverRef.removeEventListener(driverLocationListener);

        Intent i = new Intent(this, EnterAddress_Fragment.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    private void showPickupAndDestination() {
        if (pickupLat != 0 && pickupLng != 0) {
            if (pickupMarker == null) {
                pickupMarker = new Marker(mMap);
                pickupMarker.setTitle("Pickup");
                pickupMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mMap.getOverlays().add(pickupMarker);
            }
            pickupMarker.setPosition(new GeoPoint(pickupLat, pickupLng));
        }

        if (destLat != 0 && destLng != 0) {
            if (destMarker == null) {
                destMarker = new Marker(mMap);
                destMarker.setTitle("Destination");
                destMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mMap.getOverlays().add(destMarker);
            }
            destMarker.setPosition(new GeoPoint(destLat, destLng));
        }

        if (pickupLat != 0 && pickupLng != 0 && destLat != 0 && destLng != 0) {
            mMap.zoomToBoundingBox(new BoundingBox(
                    Math.max(pickupLat, destLat),
                    Math.max(pickupLng, destLng),
                    Math.min(pickupLat, destLat),
                    Math.min(pickupLng, destLng)
            ), true, 100);
        }

        mMap.invalidate();
    }
}
