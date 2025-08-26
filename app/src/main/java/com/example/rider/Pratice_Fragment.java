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
import android.widget.Toast;

import androidx.annotation.NonNull;
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

public class Pratice_Fragment extends Fragment implements OnMapReadyCallback {

    private EditText address1; // Changed from TextView to EditText
    private EditText address2;
    private Button confirmBtn;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap mMap;
    private LatLng currentLatLng;
    private String currentAddress = "";

    public Pratice_Fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pratice_, container, false);

        // Initialize views
        address1 = view.findViewById(R.id.address1);
        address2 = view.findViewById(R.id.address2);
        confirmBtn = view.findViewById(R.id.confirm_btn);

        // Initialize location services
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_view);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Get Current Location (will set as hint in address1)
        getCurrentLocation();

        // Set click listener for confirm button
        confirmBtn.setOnClickListener(v -> {
            String pickup = address1.getText().toString().trim();
            String destination = address2.getText().toString().trim();

            // If pickup is empty but we have current address, use that
            if (pickup.isEmpty() && !currentAddress.isEmpty()) {
                pickup = currentAddress;
            }

            if (destination.isEmpty()) {
                Toast.makeText(getContext(), "Please enter destination address", Toast.LENGTH_SHORT).show();
            } else {
                calculateDistanceBetweenAddresses(pickup, destination);
            }
        });

        return view;
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                currentAddress = getAddressFromLatLng(location.getLatitude(), location.getLongitude());

                // Set both hint AND text so it's visible but still editable
                address1.setHint("Current location");
                address1.setText(currentAddress);

                if (mMap != null) {
                    mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Current Location"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                }
            }
        });
    }

    private String getAddressFromLatLng(double lat, double lng) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknown Location";
    }

    private void calculateDistanceBetweenAddresses(String pickupAddress, String destinationAddress) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            // Convert pickup address to LatLng
            List<Address> pickupList = geocoder.getFromLocationName(pickupAddress, 1);
            if (pickupList == null || pickupList.isEmpty()) {
                Toast.makeText(getContext(), "Pickup location not found", Toast.LENGTH_SHORT).show();
                return;
            }
            Address pickup = pickupList.get(0);
            LatLng pickupLatLng = new LatLng(pickup.getLatitude(), pickup.getLongitude());

            // Convert destination address to LatLng
            List<Address> destList = geocoder.getFromLocationName(destinationAddress, 1);
            if (destList == null || destList.isEmpty()) {
                Toast.makeText(getContext(), "Destination not found", Toast.LENGTH_SHORT).show();
                return;
            }
            Address destination = destList.get(0);
            LatLng destLatLng = new LatLng(destination.getLatitude(), destination.getLongitude());

            // Update map with both locations
            if (mMap != null) {
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup: " + pickupAddress));
                mMap.addMarker(new MarkerOptions().position(destLatLng).title("Destination: " + destinationAddress));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pickupLatLng, 12));
            }

            // Calculate distance
            float[] results = new float[1];
            Location.distanceBetween(
                    pickupLatLng.latitude, pickupLatLng.longitude,
                    destLatLng.latitude, destLatLng.longitude,
                    results
            );
            float distanceKm = results[0] / 1000;

            // Show distance and prepare intent for next activity
            Toast.makeText(getContext(),
                    "Distance: " + String.format("%.2f km", distanceKm),
                    Toast.LENGTH_LONG).show();

            // Pass data to next activity if needed
            Intent intent = new Intent(getContext(), Address_Activity.class);
            intent.putExtra("pickup_lat", pickupLatLng.latitude);
            intent.putExtra("pickup_lng", pickupLatLng.longitude);
            intent.putExtra("pickup_address", pickupAddress);
            intent.putExtra("dest_lat", destLatLng.latitude);
            intent.putExtra("dest_lng", destLatLng.longitude);
            intent.putExtra("dest_address", destinationAddress);
            intent.putExtra("distance_km", distanceKm);
            startActivity(intent);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error processing addresses", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        // Customize map if needed
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}