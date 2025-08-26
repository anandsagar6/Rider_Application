package com.example.rider;

public class RideModel {
    public String pickupAddress;
    public String destinationAddress;
    public double pickupLat;
    public double pickupLng;
    public double destLat;
    public double destLng;
    public double distanceKm;
    public String status;

    // Empty constructor needed for Firebase
    public RideModel() {}

    public RideModel(String pickupAddress, String destinationAddress,
                     double pickupLat, double pickupLng,
                     double destLat, double destLng,
                     double distanceKm, String status) {
        this.pickupAddress = pickupAddress;
        this.destinationAddress = destinationAddress;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.destLat = destLat;
        this.destLng = destLng;
        this.distanceKm = distanceKm;
        this.status = status;
    }
}
