package com.example.rider;

public class HistoryRideModel {
    private String Drop;
    private String bookingDate;
    private String bookingTime;
    private String customerId;
    private double destLat;
    private double destLng;
    private double pickupLat;
    private double pickupLng;
    private String pickupName;
    private String pin;
    private String price;
    private String rideId;
    private String rideType;
    private String status;

    // Required empty constructor for Firebase
    public HistoryRideModel() { }

    // Getters & setters
    public String getDrop() { return Drop; }
    public void setDrop(String drop) { Drop = drop; }

    public String getBookingDate() { return bookingDate; }
    public void setBookingDate(String bookingDate) { this.bookingDate = bookingDate; }

    public String getBookingTime() { return bookingTime; }
    public void setBookingTime(String bookingTime) { this.bookingTime = bookingTime; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public double getDestLat() { return destLat; }
    public void setDestLat(double destLat) { this.destLat = destLat; }

    public double getDestLng() { return destLng; }
    public void setDestLng(double destLng) { this.destLng = destLng; }

    public double getPickupLat() { return pickupLat; }
    public void setPickupLat(double pickupLat) { this.pickupLat = pickupLat; }

    public double getPickupLng() { return pickupLng; }
    public void setPickupLng(double pickupLng) { this.pickupLng = pickupLng; }

    public String getPickupName() { return pickupName; }
    public void setPickupName(String pickupName) { this.pickupName = pickupName; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getRideId() { return rideId; }
    public void setRideId(String rideId) { this.rideId = rideId; }

    public String getRideType() { return rideType; }
    public void setRideType(String rideType) { this.rideType = rideType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
