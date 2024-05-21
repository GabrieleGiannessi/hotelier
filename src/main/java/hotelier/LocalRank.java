package hotelier;

import java.util.ArrayList;
import java.util.List;

public class LocalRank {
    private String città; //classifica riferita alla città
    private List<LocalRankHotel> listaLocalHotel;
    
    public LocalRank(String città, List<LocalRankHotel> listaLocalHotel) {
        this.città = città;
        this.listaLocalHotel = listaLocalHotel;
    }

    public LocalRank(String città) {
        this.città = città;
        this.listaLocalHotel = new ArrayList<LocalRankHotel>(); 
    }

    public String getCittà() {
        return città;
    }

    public List<LocalRankHotel> getListaLocalHotel() {
        return listaLocalHotel;
    }

    public void setListaLocalHotel(List<LocalRankHotel> listaLocalHotel) {
        this.listaLocalHotel = listaLocalHotel;
    } 

    public void addLocalHotel (LocalRankHotel h){
        if (listaLocalHotel != null) listaLocalHotel.add(h); 
    }
}
