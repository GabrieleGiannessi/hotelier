package hotelier.Structures;

import java.io.Serializable;
import java.util.Date;

public class Recensione implements Serializable{
     
    private String nomeHotel; 
    private String citta; 
    private Ratings localRates;
    private double globalRate;
    private Date data; 

    public Recensione(String nomeHotel, String città, Ratings localRates, double globalRate) {
        this.nomeHotel = nomeHotel;
        this.citta = città;
        this.localRates = localRates;
        this.globalRate = globalRate;
        this.data = new Date(); 
    }

    public String getNomeHotel() {
        return nomeHotel;
    }
    public void setNomeHotel(String nomeHotel) {
        this.nomeHotel = nomeHotel;
    }
    
    public String getCittà() {
        return citta;
    }
    public void setCittà(String città) {
        this.citta = città;
    }

    public Ratings getLocalRates() {
        return localRates;
    }
    public void setLocalRates(Ratings localRates) {
        this.localRates = localRates;
    }
    
    public double getGlobalRate() {
        return globalRate;
    }
    public void setGlobalRate(double globalRate) {
        this.globalRate = globalRate;
    }

    public Date getData() {
        return data;
    }
}
