package com.example.avancada20.ui.home;

import java.util.Objects;

public class Region {
    private String name;
    private double latitude;
    private double longitude;

    public Region(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    public Region(){}

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Region other = (Region) obj;
        return Double.compare(other.latitude, latitude) == 0 &&
                Double.compare(other.longitude, longitude) == 0 &&
                Objects.equals(other.name, name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, latitude, longitude);
    }

}
