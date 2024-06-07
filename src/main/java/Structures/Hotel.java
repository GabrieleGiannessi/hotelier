package Structures;

import java.io.Serializable;
import java.util.List;

public class Hotel implements Serializable{
    
    private int id; 
    private String name; 
    private String description;
    private String city;
    private String phone; 
    private List<String> services; 
    private double rate; 
    private Ratings ratings; 
    
    public Hotel (Ratings r){
        this.id = -1; 
        this.name = "";
        this.description = ""; 
        this.city = "";
        this.phone = "";
        this.services = null; 
        this.rate = 0; 
        this.ratings = r;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public List<String> getServices() {
        return services;
    }
    public void setServices(List<String> services) {
        this.services = services;
    }

    public double getRate() {
        return rate;
    }
    public void setRate(double rate) {
        this.rate = rate;
    }

    public Ratings getRatings() {
        return ratings;
    }
    
    public void setRatings(Ratings ratings) {
        this.ratings = ratings;
    }   

    
    public String toString() {
        return "Hotel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", city='" + city + '\'' +
                ", phone='" + phone + '\'' +
                ", services=" + services +
                ", rate=" + rate +
                ", ratings=" + ratings +
                '}';
    } 
     
    
} 