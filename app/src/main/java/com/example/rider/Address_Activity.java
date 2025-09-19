package com.example.rider;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class Address_Activity extends AppCompatActivity {

    private double pickupLat, pickupLng, destLat, destLng;
    private String pickupName, destName, Drop_address;
    private MapView mapView;
    private String selectedRideType = "";
    private double routeDistance = 0.0;

    private LinearLayout premiumLayout, sedanLayout, autoLayout, ambulanceLayout, MotoLayout;
    private Button chooseRideBtn;
    private ProgressBar progressBar;
    private TextView distanceText;

    private static final String ORS_API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImI5M2FhN2VkYjBhYTQ3NWE4YjUyOTJlZjNlNDdhMWM0IiwiaCI6Im11cm11cjY0In0=";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address);



        // Intent data
        pickupLat = getIntent().getDoubleExtra("pickup_lat", 0);
        pickupLng = getIntent().getDoubleExtra("pickup_lng", 0);
        destLat = getIntent().getDoubleExtra("dest_lat", 0);
        destLng = getIntent().getDoubleExtra("dest_lng", 0);
        pickupName = getIntent().getStringExtra("pickup_address");
        destName = getIntent().getStringExtra("dest_name");
        Drop_address = getIntent().getStringExtra("dest_address");

        // UI
        distanceText = findViewById(R.id.distance_text);
        chooseRideBtn = findViewById(R.id.choose_ride_btn);
        progressBar = findViewById(R.id.progressBar);

        premiumLayout = findViewById(R.id.premium_layout);
        sedanLayout = findViewById(R.id.sedan_layout);
        autoLayout = findViewById(R.id.auto_layout);
        ambulanceLayout = findViewById(R.id.ambulance_layout);
        MotoLayout = findViewById(R.id.Moto_layout);

        // Disable ride types and choose button until route is fetched
        setRideOptionsEnabled(false);
        chooseRideBtn.setEnabled(false);

        // Prepare clicks
        prepareContainerForClicks(premiumLayout);
        prepareContainerForClicks(sedanLayout);
        prepareContainerForClicks(autoLayout);
        prepareContainerForClicks(ambulanceLayout);

        // Set click listeners
        premiumLayout.setOnClickListener(v -> setSelectedOption(premiumLayout, "premium"));
        sedanLayout.setOnClickListener(v -> setSelectedOption(sedanLayout, "sedan"));
        autoLayout.setOnClickListener(v -> setSelectedOption(autoLayout, "auto"));
        ambulanceLayout.setOnClickListener(v -> setSelectedOption(ambulanceLayout, "ambulance"));
        MotoLayout.setOnClickListener(v -> setSelectedOption(MotoLayout, "Moto"));

        // OSM Map setup
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        mapView = findViewById(R.id.osm_map);
        mapView.setMultiTouchControls(true);
        IMapController mapController = mapView.getController();
        mapController.setZoom(14.0);

        // Markers
        GeoPoint pickupPoint = new GeoPoint(pickupLat, pickupLng);
        GeoPoint destPoint = new GeoPoint(destLat, destLng);

        Marker pickupMarker = new Marker(mapView);
        pickupMarker.setPosition(pickupPoint);
        pickupMarker.setTitle("Pickup: " + pickupName);
        mapView.getOverlays().add(pickupMarker);

        Marker destMarker = new Marker(mapView);
        destMarker.setPosition(destPoint);
        destMarker.setTitle("Destination: " + destName);
        mapView.getOverlays().add(destMarker);

        mapController.setCenter(pickupPoint);

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);

        // Fetch route and distance
        new FetchRouteTask().execute(pickupLat, pickupLng, destLat, destLng);

        // Choose ride button click
        chooseRideBtn.setOnClickListener(v -> {
            if (selectedRideType.isEmpty()) {
                Toast.makeText(this, "Please select a ride type first", Toast.LENGTH_SHORT).show();
            } else {
                progressBar.setVisibility(View.VISIBLE);
                chooseRideBtn.setEnabled(false);
                saveRideToFirebase();
            }
        });
    }

    private void setRideOptionsEnabled(boolean enabled) {
        setLayoutEnabled(premiumLayout, enabled);
        setLayoutEnabled(sedanLayout, enabled);
        setLayoutEnabled(autoLayout, enabled);
        setLayoutEnabled(ambulanceLayout, enabled);
        setLayoutEnabled(MotoLayout,enabled);
    }


    private void setLayoutEnabled(ViewGroup layout, boolean enabled) {
        layout.setEnabled(enabled);
        layout.setAlpha(enabled ? 1.0f : 0.5f); // faded look when disabled
        for (int i = 0; i < layout.getChildCount(); i++) {
            layout.getChildAt(i).setEnabled(enabled);
        }
    }


    private void setSelectedOption(LinearLayout selected, String rideType) {
        // Reset all
        premiumLayout.setSelected(false);
        sedanLayout.setSelected(false);
        autoLayout.setSelected(false);
        ambulanceLayout.setSelected(false);
        MotoLayout.setSelected(false);

        // Highlight only the selected one
        selected.setSelected(true);

        // Update ride type
        selectedRideType = rideType;
        chooseRideBtn.setText("Choose " + rideType.substring(0,1).toUpperCase() + rideType.substring(1));

        if(chooseRideBtn.isSelected()){
            chooseRideBtn.setVisibility(View.GONE);
        }
    }



    private void prepareContainerForClicks(LinearLayout layout) {
        layout.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        layout.setClickable(true);
        layout.setFocusable(true);
        for (int i=0; i<layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            child.setClickable(true);
            child.setOnClickListener(v -> layout.performClick());
        }
    }

    private class FetchRouteTask extends AsyncTask<Double, Void, RouteInfo> {
        @Override
        protected RouteInfo doInBackground(Double... params) {
            double startLat = params[0];
            double startLng = params[1];
            double endLat = params[2];
            double endLng = params[3];

            try {
                String urlStr = String.format(Locale.US,
                        "https://api.openrouteservice.org/v2/directions/driving-car?api_key=%s&start=%f,%f&end=%f,%f",
                        ORS_API_KEY, startLng, startLat, endLng, endLat);

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                JSONArray coordinates = json.getJSONArray("features")
                        .getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates");

                List<GeoPoint> routePoints = new ArrayList<>();
                for (int i=0; i<coordinates.length(); i++) {
                    JSONArray coord = coordinates.getJSONArray(i);
                    double lon = coord.getDouble(0);
                    double lat = coord.getDouble(1);
                    routePoints.add(new GeoPoint(lat, lon));
                }

                double distanceMeters = json.getJSONArray("features")
                        .getJSONObject(0)
                        .getJSONObject("properties")
                        .getJSONObject("summary")
                        .getDouble("distance");
                double distanceKm = distanceMeters / 1000.0;

                return new RouteInfo(routePoints, distanceKm);

            } catch(Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(RouteInfo routeInfo) {
            progressBar.setVisibility(View.GONE);

            if (routeInfo != null && !routeInfo.routePoints.isEmpty()) {
                routeDistance = routeInfo.distanceKm;
                DecimalFormat df = new DecimalFormat("0.00");

                // Draw route
                Polyline line = new Polyline();
                line.setPoints(routeInfo.routePoints);
                line.setColor(getResources().getColor(R.color.lavender));
                line.setWidth(10f);
                mapView.getOverlays().add(line);
                mapView.invalidate();

                distanceText.setText("Distance: " + df.format(routeDistance) + " km");

                // Set ride prices dynamically
                ((TextView)findViewById(R.id.auto_price)).setText("₹" + df.format(8 * routeDistance));
                ((TextView)findViewById(R.id.sedan_price)).setText("₹" + df.format(10 * routeDistance));
                ((TextView)findViewById(R.id.premium_price)).setText("₹" + df.format(12 * routeDistance));
                ((TextView)findViewById(R.id.abmulance_price)).setText("₹" + df.format(15 * routeDistance));
                ((TextView)findViewById(R.id.Moto_price)).setText("₹" + df.format(4 * routeDistance));

                // Enable ride options and choose button
                setRideOptionsEnabled(true);
                chooseRideBtn.setEnabled(true);

            } else {
                Toast.makeText(Address_Activity.this, "Failed to fetch route", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveRideToFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String customerId = user.getUid();
        DatabaseReference ridesRef = FirebaseDatabase.getInstance().getReference("Rides");
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference("Customers")
                .child(customerId).child("rides");

        String rideId = ridesRef.push().getKey();
        if (rideId == null) return;

        DecimalFormat df = new DecimalFormat("0.00");
        String price = "0";
        switch(selectedRideType){
            case "premium": price = ((TextView)findViewById(R.id.premium_price)).getText().toString(); break;
            case "sedan": price = ((TextView)findViewById(R.id.sedan_price)).getText().toString(); break;
            case "auto": price = ((TextView)findViewById(R.id.auto_price)).getText().toString(); break;
            case "ambulance": price = ((TextView)findViewById(R.id.abmulance_price)).getText().toString(); break;
            case "Moto": price = ((TextView)findViewById(R.id.Moto_price)).getText().toString(); break;
        }

        HashMap<String,Object> rideData = new HashMap<>();
        rideData.put("rideId", rideId);
        rideData.put("pickupName", pickupName != null ? pickupName : "Unknown Pickup");
        rideData.put("dropAddress", Drop_address != null ? Drop_address : "Unknown Drop");
        rideData.put("pickupLat", pickupLat);
        rideData.put("pickupLng", pickupLng);
        rideData.put("destLat", destLat);
        rideData.put("destLng", destLng);
        rideData.put("rideType", selectedRideType);
        rideData.put("price", price);
        rideData.put("distance", df.format(routeDistance));
        rideData.put("status","waiting");
        rideData.put("customerId", customerId);
        rideData.put("bookingDate", new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(new Date()));
        rideData.put("bookingTime", new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date()));


        // ✅ Add placeholder for driver details
        rideData.put("driverId", "not_assigned");
        rideData.put("driverName", "Searching for Driver");




        final String finalPrice = price;

        // Save in both locations
        // Save in both locations safely
        ridesRef.child(rideId).updateChildren(rideData);
        customerRef.child(rideId).updateChildren(rideData).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            chooseRideBtn.setEnabled(true);
            if(task.isSuccessful()){
                Intent intent = new Intent(this, WaitForDriver_Activity.class);
                intent.putExtra("ride_type", selectedRideType);
                intent.putExtra("price", finalPrice);
                intent.putExtra("pickup_name", pickupName);
                intent.putExtra("drop_address", Drop_address);
                intent.putExtra("pickup_lat", pickupLat);
                intent.putExtra("pickup_lng", pickupLng);
                intent.putExtra("dest_lat", destLat);
                intent.putExtra("dest_lng", destLng);
                intent.putExtra("ride_id", rideId);
                intent.putExtra("distance", df.format(routeDistance));
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } else {
                Toast.makeText(this,"Failed to confirm ride", Toast.LENGTH_SHORT).show();
            }
        });


        
    }


    private static class RouteInfo {
        List<GeoPoint> routePoints;
        double distanceKm;
        RouteInfo(List<GeoPoint> routePoints, double distanceKm){
            this.routePoints = routePoints;
            this.distanceKm = distanceKm;
        }
    }

    private void onDriverAccept(String rideId, String driverId, String driverName, double driverLat, double driverLng) {
        DatabaseReference ridesRef = FirebaseDatabase.getInstance().getReference("Rides").child(rideId);
        DatabaseReference customersRef = FirebaseDatabase.getInstance().getReference("Customers");
        DatabaseReference driversRef = FirebaseDatabase.getInstance().getReference("drivers").child(driverId).child("rides");

        String acceptTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        // ✅ Update ride info in Rides
        HashMap<String, Object> rideUpdate = new HashMap<>();
        rideUpdate.put("driverId", driverId);
        rideUpdate.put("driverName", driverName);
        rideUpdate.put("driverLat", driverLat);
        rideUpdate.put("driverLng", driverLng);
        rideUpdate.put("driverAcceptTime", acceptTime);
        rideUpdate.put("status", "accepted");

        ridesRef.updateChildren(rideUpdate).addOnSuccessListener(aVoid -> {
            ridesRef.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    // ✅ Get customerId
                    String customerId = snapshot.child("customerId").getValue(String.class);

                    if (customerId != null) {
                        // update in Customers
                        customersRef.child(customerId).child("rides").child(rideId).updateChildren(rideUpdate);

                        // fetch customer details
                        customersRef.child(customerId).get().addOnSuccessListener(customerSnap -> {
                            String customerName = customerSnap.child("name").getValue(String.class);
                            String customerPhone = customerSnap.child("phone").getValue(String.class);

                            // ✅ Add full ride + customer data for driver
                            HashMap<String, Object> driverRideData = new HashMap<>(rideUpdate);
                            driverRideData.put("pickupName", snapshot.child("pickupName").getValue(String.class));
                            driverRideData.put("dropAddress", snapshot.child("dropAddress").getValue(String.class));
                            driverRideData.put("destLat", snapshot.child("destLat").getValue(Double.class));
                            driverRideData.put("destLng", snapshot.child("destLng").getValue(Double.class));
                            driverRideData.put("price", snapshot.child("price").getValue(String.class));
                            driverRideData.put("rideType", snapshot.child("rideType").getValue(String.class));

                            // ✅ add customer info
                            driverRideData.put("customerId", customerId);
                            driverRideData.put("customerName", customerName);
                            driverRideData.put("customerPhone", customerPhone);

                            // save in driver node
                            driversRef.child(rideId).setValue(driverRideData);
                        });
                    }
                }
            });
        });
    }


}
