package com.example.rider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class Enter_Address_Activity extends AppCompatActivity implements OnMapReadyCallback {

    private EditText etPickupLocation, etDestination;
    private Button btnConfirm;

    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap googleMap;
    private LatLng currentLatLng;
    private String currentAddress = "";

    private static final int REQUEST_LOCATION_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_address);

        etPickupLocation = findViewById(R.id.address1);
        etDestination = findViewById(R.id.address2);
        btnConfirm = findViewById(R.id.confirm_btn);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_view);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fetchCurrentLocation();

        btnConfirm.setOnClickListener(v -> validateAndCalculateDistance());
    }

    private void fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        handleLocationUpdate(location);
                    } else {
                        Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleLocationUpdate(Location location) {
        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        currentAddress = getAddressFromCoordinates(location.getLatitude(), location.getLongitude());

        etPickupLocation.setHint("Current location");
        etPickupLocation.setText(currentAddress);

        if (googleMap != null) {
            updateMapWithCurrentLocation();
        }
    }

    private void updateMapWithCurrentLocation() {
        googleMap.addMarker(new MarkerOptions()
                .position(currentLatLng)
                .title("You are here"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
    }

    private void validateAndCalculateDistance() {
        String pickup = etPickupLocation.getText().toString().trim();
        String destination = etDestination.getText().toString().trim();

        if (pickup.isEmpty() && !currentAddress.isEmpty()) {
            pickup = currentAddress;
            etPickupLocation.setText(pickup);
        }

        if (destination.isEmpty()) {
            etDestination.setError("Please enter destination");
            return;
        }

        calculateDistance(pickup, destination);
    }

    private void calculateDistance(String pickupAddress, String destinationAddress) {
        new Thread(() -> {
            try {
                LatLng pickupLatLng = getLatLngFromAddress(pickupAddress, "Pickup");
                LatLng destLatLng = getLatLngFromAddress(destinationAddress, "Destination");

                if (pickupLatLng == null || destLatLng == null) return;

                float distance = calculateDistanceBetween(pickupLatLng, destLatLng);

                runOnUiThread(() -> {
                    updateMapWithBothLocations(pickupLatLng, destLatLng, pickupAddress, destinationAddress);
                    showDistanceResult(distance);
                    navigateToNextActivity(pickupLatLng, destLatLng, pickupAddress, destinationAddress, distance);
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private LatLng getLatLngFromAddress(String address, String type) throws IOException {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = geocoder.getFromLocationName(address, 1);

        if (addresses == null || addresses.isEmpty()) {
            runOnUiThread(() ->
                    Toast.makeText(this, type + " location not found", Toast.LENGTH_SHORT).show());
            return null;
        }

        Address result = addresses.get(0);
        return new LatLng(result.getLatitude(), result.getLongitude());
    }

    private float calculateDistanceBetween(LatLng origin, LatLng destination) {
        float[] results = new float[1];
        Location.distanceBetween(
                origin.latitude, origin.longitude,
                destination.latitude, destination.longitude,
                results
        );
        return results[0] / 1000;
    }

    private void updateMapWithBothLocations(LatLng pickup, LatLng destination,
                                            String pickupAddr, String destAddr) {
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions()
                .position(pickup)
                .title("Pickup: " + pickupAddr));
        googleMap.addMarker(new MarkerOptions()
                .position(destination)
                .title("Destination: " + destAddr));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pickup, 12f));
    }

    private void showDistanceResult(float distanceKm) {
        Toast.makeText(this,
                String.format("Distance: %.2f km", distanceKm),
                Toast.LENGTH_LONG).show();
    }

    private void navigateToNextActivity(LatLng pickup, LatLng destination,
                                        String pickupAddr, String destAddr, float distance) {
        Intent intent = new Intent(this, Address_Activity.class);
        intent.putExtra("pickup_lat", pickup.latitude);
        intent.putExtra("pickup_lng", pickup.longitude);
        intent.putExtra("pickup_address", pickupAddr);
        intent.putExtra("dest_lat", destination.latitude);
        intent.putExtra("dest_lng", destination.longitude);
        intent.putExtra("dest_address", destAddr); // <-- address2 passed
        intent.putExtra("distance_km", distance);
        intent.putExtra("pickup", etPickupLocation.getText().toString().trim());
        intent.putExtra("destination_address", etDestination.getText().toString().trim()); // <-- also passing address2 explicitly
        startActivity(intent);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);

        if (currentLatLng != null) {
            updateMapWithCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getAddressFromCoordinates(double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Current Location";
    }
}
