package com.example.rider;

public class Customer {
    public String name, phone, email;
    public int pin;

    public Customer() {} // required for Firebase

    public Customer(String name, String phone, String email, int pin) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.pin = pin;
    }
}
