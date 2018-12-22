package com.lookingbus.chaithra.demo;

public class LocationModel {
    Double lat;
    Double log;

    public LocationModel(Double lat, Double log) {
        this.lat = lat;
        this.log = log;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLog() {
        return log;
    }

    public void setLog(Double log) {
        this.log = log;
    }
}
