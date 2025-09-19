package com.example.rider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class EnterAddress_Fragment extends Fragment {

    private EditText etPickupLocation, etDestination;
    private Button btnConfirm;
    private ProgressBar progressBar;

    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private GeoPoint currentGeoPoint;
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

        // osmdroid setup
        Configuration.getInstance().load(requireContext().getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext().getApplicationContext()));

        mapView = view.findViewById(R.id.osm_map);
        mapView.setMultiTouchControls(true);
        IMapController mapController = mapView.getController();
        mapController.setZoom(15.0);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // If last drop passed from Home_Fragment, set it in Destination box
        if (getArguments() != null) {
            String lastDrop = getArguments().getString("lastDrop", "");
            if (!lastDrop.isEmpty()) {
                String cityState = getCityStateFromName(lastDrop);
                etDestination.setText(cityState);
            }
        }

        fetchCurrentLocation();

        btnConfirm.setOnClickListener(v -> validateAndCalculateDistance());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (btnConfirm != null) btnConfirm.setVisibility(View.VISIBLE);
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    private void handleLocationUpdate(Location location) {
        currentGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        currentAddress = getAddressFromCoordinates(location.getLatitude(), location.getLongitude());

        etPickupLocation.setHint("Current location");
        etPickupLocation.setText(currentAddress);

        updateMapWithCurrentLocation();
    }

    private void updateMapWithCurrentLocation() {
        mapView.getOverlays().clear();

        if (currentGeoPoint != null) {
            IMapController mapController = mapView.getController();
            mapController.setCenter(currentGeoPoint);

            Marker startMarker = new Marker(mapView);
            startMarker.setPosition(currentGeoPoint);
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            startMarker.setTitle("You are here");
            mapView.getOverlays().add(startMarker);
        }
        mapView.invalidate();
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

        btnConfirm.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        calculateDistance(pickup, destination);
    }

    private void calculateDistance(String pickupAddress, String destinationAddress) {
        new Thread(() -> {
            try {
                GeoPoint pickupPoint = getGeoPointFromAddress(pickupAddress, "Pickup");
                GeoPoint destPoint = getGeoPointFromAddress(destinationAddress, "Destination");

                if (pickupPoint == null || destPoint == null) {
                    requireActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                    return;
                }

                float distance = calculateDistanceBetween(pickupPoint, destPoint);

                requireActivity().runOnUiThread(() -> {
                    updateMapWithBothLocations(pickupPoint, destPoint, pickupAddress, destinationAddress);

                    progressBar.setVisibility(View.GONE);
                    navigateToNextActivity(pickupPoint, destPoint, pickupAddress, destinationAddress, distance);
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnConfirm.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private GeoPoint getGeoPointFromAddress(String address, String type) throws IOException {
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
        return new GeoPoint(result.getLatitude(), result.getLongitude());
    }

    private float calculateDistanceBetween(GeoPoint origin, GeoPoint destination) {
        float[] results = new float[1];
        Location.distanceBetween(
                origin.getLatitude(), origin.getLongitude(),
                destination.getLatitude(), destination.getLongitude(),
                results
        );
        return results[0] / 1000;
    }

    private void updateMapWithBothLocations(GeoPoint pickup, GeoPoint destination,
                                            String pickupAddr, String destAddr) {
        mapView.getOverlays().clear();

        Marker pickupMarker = new Marker(mapView);
        pickupMarker.setPosition(pickup);
        pickupMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        pickupMarker.setTitle("Pickup: " + pickupAddr);
        mapView.getOverlays().add(pickupMarker);

        Marker destMarker = new Marker(mapView);
        destMarker.setPosition(destination);
        destMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        destMarker.setTitle("Destination: " + destAddr);
        mapView.getOverlays().add(destMarker);

        IMapController mapController = mapView.getController();
        mapController.setCenter(pickup);
        mapController.setZoom(12.0);

        mapView.invalidate();
    }



    private void navigateToNextActivity(GeoPoint pickup, GeoPoint destination,
                                        String pickupAddr, String destAddr, float distance) {
        Intent intent = new Intent(requireActivity(), Address_Activity.class);
        intent.putExtra("pickup_lat", pickup.getLatitude());
        intent.putExtra("pickup_lng", pickup.getLongitude());
        intent.putExtra("pickup_address", pickupAddr);
        intent.putExtra("dest_lat", destination.getLatitude());
        intent.putExtra("dest_lng", destination.getLongitude());
        intent.putExtra("dest_address", destAddr);
        intent.putExtra("distance_km", distance);
        requireActivity().startActivity(intent);
        requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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
                return state != null ? city + ", " + state : city;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return placeName;
    }

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
