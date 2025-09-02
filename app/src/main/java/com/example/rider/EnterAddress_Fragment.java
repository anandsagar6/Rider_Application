package com.example.rider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

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

public class EnterAddress_Fragment extends Fragment implements OnMapReadyCallback {

    private EditText etPickupLocation, etDestination;
    private Button btnConfirm;
    private ProgressBar progressBar;

    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap googleMap;
    private LatLng currentLatLng;
    private String currentAddress = "";

    private static final int REQUEST_LOCATION_PERMISSION = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_enter_address_, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etPickupLocation = view.findViewById(R.id.address1);
        etDestination = view.findViewById(R.id.address2);
        btnConfirm = view.findViewById(R.id.confirm_btn);
        progressBar = view.findViewById(R.id.progressBar);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // ✅ If last drop passed from Home_Fragment, set it in Destination box
        if (getArguments() != null) {
            String lastDrop = getArguments().getString("lastDrop", "");
            if (!lastDrop.isEmpty()) {
                String cityState = getCityStateFromName(lastDrop);
                etDestination.setText(cityState);
            }
        }

        // Map setup
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_view);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fetchCurrentLocation();

        btnConfirm.setOnClickListener(v -> validateAndCalculateDistance());
    }

    @Override
    public void onResume() {
        super.onResume();
        // ✅ Reset UI when coming back
        if (btnConfirm != null) btnConfirm.setVisibility(View.VISIBLE);
        if (progressBar != null) progressBar.setVisibility(View.GONE);
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
        googleMap.clear(); // ✅ prevent duplicate markers
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

        // ✅ Hide button and show progress bar
        btnConfirm.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        calculateDistance(pickup, destination);
    }

    private void calculateDistance(String pickupAddress, String destinationAddress) {
        new Thread(() -> {
            try {
                LatLng pickupLatLng = getLatLngFromAddress(pickupAddress, "Pickup");
                LatLng destLatLng = getLatLngFromAddress(destinationAddress, "Destination");

                if (pickupLatLng == null || destLatLng == null) {
                    requireActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                    return;
                }

                float distance = calculateDistanceBetween(pickupLatLng, destLatLng);

                requireActivity().runOnUiThread(() -> {
                    updateMapWithBothLocations(pickupLatLng, destLatLng, pickupAddress, destinationAddress);
                    showDistanceResult(distance);
                    progressBar.setVisibility(View.GONE);
                    navigateToNextActivity(pickupLatLng, destLatLng, pickupAddress, destinationAddress, distance);
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnConfirm.setVisibility(View.VISIBLE); // ✅ show again on error
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private LatLng getLatLngFromAddress(String address, String type) throws IOException {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        List<Address> addresses = geocoder.getFromLocationName(address, 1);

        if (addresses == null || addresses.isEmpty()) {
            requireActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                btnConfirm.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(), type + " location not found", Toast.LENGTH_SHORT).show();
            });
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
        Toast.makeText(requireContext(),
                String.format("Distance: %.2f km", distanceKm),
                Toast.LENGTH_LONG).show();
    }

    private void navigateToNextActivity(LatLng pickup, LatLng destination,
                                        String pickupAddr, String destAddr, float distance) {
        Intent intent = new Intent(requireActivity(), Address_Activity.class);
        intent.putExtra("pickup_lat", pickup.latitude);
        intent.putExtra("pickup_lng", pickup.longitude);
        intent.putExtra("pickup_address", pickupAddr);
        intent.putExtra("dest_lat", destination.latitude);
        intent.putExtra("dest_lng", destination.longitude);
        intent.putExtra("dest_address", destAddr);
        intent.putExtra("distance_km", distance);
        intent.putExtra("pickup", etPickupLocation.getText().toString().trim());
        intent.putExtra("destination_address", etDestination.getText().toString().trim());
        requireActivity().startActivity(intent);
        requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
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
                Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getAddressFromCoordinates(double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Current Location";
    }

    private String getCityStateFromName(String placeName) {
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(placeName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);

                String city = addr.getLocality();
                if (city == null) city = placeName;
                String state = addr.getAdminArea();

                if (state != null) {
                    return city + ", " + state;
                } else {
                    return city;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return placeName;
    }

    // ✅ Dummy method for location fetch (you should have real implementation)
    private void fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) handleLocationUpdate(location);
                });
    }
}
