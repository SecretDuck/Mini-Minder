package com.tmung.miniminder;

public class LocationData {
    private double latitude;
    private double longitude;
    float radius;
    private String id;

    // Needed for Firebase's automatic data mapping
    public LocationData() { }

    public LocationData(double latitude, double longitude, float radius) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius  = radius;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}

