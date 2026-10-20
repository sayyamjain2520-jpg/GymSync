package com.example.gymsync;

import java.io.Serializable;
import java.util.Locale;

public class GymModel implements Serializable {
    private String id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private float distanceKm;
    private double rating;
    private int userRatingsTotal;
    private String openStatus;
    private String phone;
    private String website;
    private boolean isFromApi;

    public GymModel(String id, String name, String address, double latitude, double longitude,
                    float distanceKm, double rating, int userRatingsTotal, String openStatus,
                    String phone, String website, boolean isFromApi) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceKm = distanceKm;
        this.rating = rating;
        this.userRatingsTotal = userRatingsTotal;
        this.openStatus = openStatus;
        this.phone = phone;
        this.website = website;
        this.isFromApi = isFromApi;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public float getDistanceKm() { return distanceKm; }
    public void setDistanceKm(float distanceKm) { this.distanceKm = distanceKm; }
    public double getRating() { return rating; }
    public int getUserRatingsTotal() { return userRatingsTotal; }
    public String getOpenStatus() { return openStatus; }
    public String getPhone() { return phone; }
    public String getWebsite() { return website; }
    public boolean isFromApi() { return isFromApi; }

    public boolean hasRating() {
        return rating > 0.0;
    }

    public String getFormattedRating() {
        if (!hasRating()) {
            return "Unrated";
        }
        return String.format(Locale.getDefault(), "%.1f", rating);
    }

    public String getFormattedDistance() {
        if (distanceKm <= 0) {
            return "Nearby";
        }
        if (distanceKm < 1.0f) {
            return String.format(Locale.getDefault(), "%d m away", Math.round(distanceKm * 1000));
        }
        return String.format(Locale.getDefault(), "%.1f km away", distanceKm);
    }
}
