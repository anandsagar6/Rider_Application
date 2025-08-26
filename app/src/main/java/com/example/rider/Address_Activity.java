package com.example.rider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class Address_Activity extends AppCompatActivity implements OnMapReadyCallback {

    private double pickupLat, pickupLng, destLat, destLng;
    private String pickupName, destName, Drop_address;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private String selectedRideType = "";

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;

    // UI
    private LinearLayout premiumLayout, sedanLayout, autoLayout;
    private Button chooseRideBtn;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address);

        float distanceKm = getIntent().getFloatExtra("distance_km", 0f);

        // Init bottom sheet
        LinearLayout bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(dpToPx(350));
        bottomSheetBehavior.setFitToContents(true);
        bottomSheetBehavior.setDraggable(true);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setHalfExpandedRatio(0.6f);

        chooseRideBtn = findViewById(R.id.choose_ride_btn);
        progressBar = findViewById(R.id.progressBar);

        // Bottom sheet callbacks
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View sheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    chooseRideBtn.setText("Confirm Ride");
                }
            }

            @Override
            public void onSlide(@NonNull View sheet, float slideOffset) {
                sheet.setAlpha(0.7f + (0.3f * slideOffset));
            }
        });

        // Price calculation
        TextView premiumPrice = findViewById(R.id.premium_price);
        TextView sedanPrice = findViewById(R.id.sedan_price);
        TextView autoPrice = findViewById(R.id.auto_price);

        float auto = (8 * distanceKm);
        float premium = (12 * distanceKm);
        float sedan = (10 * distanceKm);

        premiumPrice.setText(String.format("₹%.2f", premium));
        sedanPrice.setText(String.format("₹%.2f", sedan));
        autoPrice.setText(String.format("₹%.2f", auto));

        // Ride option layouts
        premiumLayout = findViewById(R.id.premium_layout);
        sedanLayout = findViewById(R.id.sedan_layout);
        autoLayout = findViewById(R.id.auto_layout);

        prepareContainerForClicks(premiumLayout);
        prepareContainerForClicks(sedanLayout);
        prepareContainerForClicks(autoLayout);

        premiumLayout.setOnClickListener(v -> setSelectedOption(premiumLayout, "premium"));
        sedanLayout.setOnClickListener(v -> setSelectedOption(sedanLayout, "sedan"));
        autoLayout.setOnClickListener(v -> setSelectedOption(autoLayout, "auto"));

        // Intent data
        pickupLat = getIntent().getDoubleExtra("pickup_lat", 0);
        pickupLng = getIntent().getDoubleExtra("pickup_lng", 0);
        destLat = getIntent().getDoubleExtra("dest_lat", 0);
        destLng = getIntent().getDoubleExtra("dest_lng", 0);
        pickupName = getIntent().getStringExtra("pickup");
        destName = getIntent().getStringExtra("dest_name");
        Drop_address = getIntent().getStringExtra("destination_address");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_view);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Button click
        chooseRideBtn.setOnClickListener(v -> {
            if (selectedRideType.isEmpty()) {
                Toast.makeText(this, "Please select a ride option first", Toast.LENGTH_SHORT).show();
            } else {
                chooseRideBtn.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                navigateToWaitForDriver();
            }
        });
    }

    private void setSelectedOption(LinearLayout selected, String rideType) {
        premiumLayout.setSelected(false);
        sedanLayout.setSelected(false);
        autoLayout.setSelected(false);
        selected.setSelected(true);
        selectedRideType = rideType;

        chooseRideBtn.setText("Choose " + rideType.substring(0, 1).toUpperCase() + rideType.substring(1));
    }

    private void navigateToWaitForDriver() {
        String price = "";
        switch (selectedRideType) {
            case "premium":
                price = ((TextView) findViewById(R.id.premium_price)).getText().toString();
                break;
            case "sedan":
                price = ((TextView) findViewById(R.id.sedan_price)).getText().toString();
                break;
            case "auto":
                price = ((TextView) findViewById(R.id.auto_price)).getText().toString();
                break;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String customerId = user.getUid();

            DatabaseReference ridesRef = FirebaseDatabase.getInstance().getReference("Rides");
            DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference("Customers")
                    .child(customerId).child("rides");

            String rideId = ridesRef.push().getKey();

            DatabaseReference customerPinRef = FirebaseDatabase.getInstance().getReference("Customers")
                    .child(customerId).child("pin");

            String finalPrice1 = price;
            customerPinRef.get().addOnSuccessListener(snapshot -> {
                String customerPin = snapshot.exists() ? snapshot.getValue(String.class) : null;

                String bookingDate = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(new Date());
                String bookingTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

                HashMap<String, Object> rideData = new HashMap<>();
                rideData.put("rideId", rideId);
                rideData.put("pickupName", pickupName);
                rideData.put("dropName", destName);
                rideData.put("pickupLat", pickupLat);
                rideData.put("pickupLng", pickupLng);
                rideData.put("destLat", destLat);
                rideData.put("destLng", destLng);
                rideData.put("rideType", selectedRideType);
                rideData.put("Drop", Drop_address);
                rideData.put("price", finalPrice1);
                rideData.put("status", "waiting");
                rideData.put("customerId", customerId);
                rideData.put("pin", customerPin);
                rideData.put("bookingDate", bookingDate);
                rideData.put("bookingTime", bookingTime);

                if (rideId != null) {
                    ridesRef.child(rideId).setValue(rideData);
                    String finalPrice = finalPrice1;
                    customerRef.child(rideId).setValue(rideData).addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        chooseRideBtn.setVisibility(View.VISIBLE);

                        if (task.isSuccessful()) {
                            Intent intent = new Intent(this, WaitForDriver_Activity.class);
                            intent.putExtra("ride_type", selectedRideType);
                            intent.putExtra("price", finalPrice);
                            intent.putExtra("pickup_name", pickupName);
                            intent.putExtra("dest_name", destName);
                            intent.putExtra("pickup_lat", pickupLat);
                            intent.putExtra("pickup_lng", pickupLng);
                            intent.putExtra("dest_lat", destLat);
                            intent.putExtra("dest_lng", destLng);
                            intent.putExtra("ride_id", rideId);
                            intent.putExtra("d", Drop_address);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Failed to confirm ride, try again", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
    }

    private void prepareContainerForClicks(LinearLayout layout) {
        layout.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        layout.setClickable(true);
        layout.setFocusable(true);

        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            child.setClickable(true);
            child.setOnClickListener(v -> layout.performClick());
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            enableMyLocation();
        }

        LatLng pickup = new LatLng(pickupLat, pickupLng);
        mMap.addMarker(new MarkerOptions()
                .position(pickup)
                .title("Pickup: " + pickupName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        LatLng destination = new LatLng(destLat, destLng);
        mMap.addMarker(new MarkerOptions()
                .position(destination)
                .title("Destination: " + destName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        mMap.addPolyline(new PolylineOptions()
                .add(pickup, destination)
                .width(12)
                .color(ContextCompat.getColor(this, R.color.colorPrimary)));

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(pickup);
        builder.include(destination);
        LatLngBounds bounds = builder.build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 14));
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (chooseRideBtn != null) chooseRideBtn.setVisibility(View.VISIBLE);
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
