package hotelier;

//questa classe serve per poter referire le posizioni degli hotel
public class LocalRankHotel {
    private Hotel h; 
    private int rank; //posizione all'interno della classifica
    
    public LocalRankHotel(Hotel h, int rank) {
        this.h = h;
        this.rank = rank;
    }

    public Hotel getH() {
        return h;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }   
}
