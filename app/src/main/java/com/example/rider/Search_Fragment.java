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
import android.widget.TextView;
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

public class Search_Fragment extends Fragment implements OnMapReadyCallback {

    private TextView address1;
    private EditText address2;
    private Button confirmBtn;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap mMap;
    private LatLng currentLatLng;

    public Search_Fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_search_, container, false);

        address1 = view.findViewById(R.id.address1);
        address2 = view.findViewById(R.id.address2);
        confirmBtn = view.findViewById(R.id.confirm_btn);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Get Current Location
        getCurrentLocation();

        // On Confirm Button Click
        confirmBtn.setOnClickListener(v -> {
            String destination = address2.getText().toString().trim();
            if (destination.isEmpty()) {
                Toast.makeText(getContext(), "Enter destination address", Toast.LENGTH_SHORT).show();
            } else {
                handleDestination(destination);
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
                String currentAddress = getAddressFromLatLng(location.getLatitude(), location.getLongitude());
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

    private void handleDestination(String destination) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> destList = geocoder.getFromLocationName(destination, 1);
            if (destList != null && !destList.isEmpty()) {
                Address destAddress = destList.get(0);
                LatLng destLatLng = new LatLng(destAddress.getLatitude(), destAddress.getLongitude());

                if (mMap != null) {
                    mMap.addMarker(new MarkerOptions().position(destLatLng).title("Destination"));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destLatLng, 12));
                }

                float distanceKm = 0;
                if (currentLatLng != null) {
                    float[] results = new float[1];
                    Location.distanceBetween(
                            currentLatLng.latitude, currentLatLng.longitude,
                            destLatLng.latitude, destLatLng.longitude,
                            results
                    );
                    distanceKm = results[0] / 1000;
                    Toast.makeText(getContext(), "Distance: " + String.format("%.2f", distanceKm) + " km", Toast.LENGTH_LONG).show();
                }

                String drop = address2.getText().toString();
                String pickup = address1.getText().toString();

                Intent intent = new Intent(getContext(), Address_Activity.class);
                intent.putExtra("pickup_lat", currentLatLng.latitude);
                intent.putExtra("pickup_lng", currentLatLng.longitude);
                intent.putExtra("pickup", pickup);
                intent.putExtra("dest_lat", destAddress.getLatitude());
                intent.putExtra("dest_lng", destAddress.getLongitude());
                intent.putExtra("dest_name", destination);
                intent.putExtra("distance_km", distanceKm);
                intent.putExtra("address2", drop);
                startActivity(intent);

            } else {
                Toast.makeText(getContext(), "Destination not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }
}
