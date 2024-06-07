package Structures;

import java.io.Serializable;

public class Ratings implements Serializable{

    private double services;
    private double quality;
    private double cleaning;
    private double position;

    public Ratings(double cleaning, double position, double services, double quality) {
        this.cleaning = cleaning;
        this.position = position;
        this.services = services;
        this.quality = quality;
    }

    
    public double getCleaning() {
        return cleaning;
    }

    public void setCleaning(double cleaning) {
        this.cleaning = cleaning;
    }


    public double getPosition() {
        return position;
    }

    public void setPosition(double position) {
        this.position = position;
    }

    public double getServices() {
        return services;
    }

    public void setServices(double services) {
        this.services = services;
    }


    public double getQuality() {
        return quality;
    }

    public void setQuality(double quality) {
        this.quality = quality;
    }
}
