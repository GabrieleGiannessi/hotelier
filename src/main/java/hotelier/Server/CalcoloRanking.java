package hotelier.Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import hotelier.Structures.Hotel;
import hotelier.Structures.LocalRank;
import hotelier.Structures.LocalRankHotel;
import hotelier.Structures.Ratings;
import hotelier.Structures.Recensione;

/**
 * Task che esegue il calcolo del ranking e si occupa di mandare un messaggio UDP broadcast agli utenti loggati  
 */

public class CalcoloRanking implements Runnable {

    private MulticastSocket m; 
    private String group;
    private int port; 
    private JsonDB db; 

    public CalcoloRanking (MulticastSocket m, String group, int port, JsonDB db){
        this.m = m; 
        this.group = group;
        this.port = port; 
        this.db = db; 
    }

    @Override
    public void run() {
         
        List <LocalRank> before = db.scanLocalRankings(); //faccio una scansione dei ranking prima di aggiornare i valori dei rate degli hotel
        if (before == null){
            makeLocalRankings();
            before = db.scanLocalRankings();
        } 
        
        aggiornaRatingHotels(); //si aggiornano i rates (voti) degli hotel
        List <LocalRank> after = aggiornaRankingLocali(); //ricalcolo le posizioni degli hotel in base alle recensioni (del momento)


        //bisogna mandare a questo punto le notifiche al client se sono cambiati qualche primo posto nei rank locali

        for (LocalRank i : after){
            for (LocalRank j : before){
                if (j.getCittà().equals(i.getCittà())){
                    //controlliamo se la posizione cambia
                    LocalRankHotel i_after = i.searchHotelByRank(1); 
                    LocalRankHotel i_before = j.searchHotelByRank(1);

                    if (!i_after.getH().getName().equals(i_before.getH().getName())){
                        //mandiamo la notifica
                        try{
                            String mess = "Ranking locale cambiato per "+ i.getCittà() + " : "+i_after.getH().getName()+ " \n";
                            DatagramPacket dp = new DatagramPacket(mess.getBytes(), mess.length(), InetAddress.getByName(group), port);
                            m.send(dp);
                        }catch (IOException e){
                            System.err.println("Errore durante trasferimento messaggio UDP");
                            e.printStackTrace();
                        }
                    }

                  break;   
                }
            }
        }              
    }

    /**
     * Attraverso questa funzione vengono aggiornati i voti (sia globali che più specifici) riferiti agli hotel, in cui vengono considerate anche le date in cui le recensioni
     * sono state fatte 
     */

    public void aggiornaRatingHotels (){
        List <Hotel> hotels = db.scanHotels(); 
        List <Recensione> recensioni = db.scanRecensioni(); 

        for (Hotel h : hotels){
            List <Recensione> recensioniHotel = new ArrayList<Recensione>(); 
            for (Recensione r : recensioni){
                if (r.getCittà().equals(h.getCity()) && r.getNomeHotel().equals(h.getName())){
                    recensioniHotel.add (r); 
                }
            }

            if (recensioniHotel != null){
                //salviamo i nuovi rate all'hotel
                Ratings ratings = makeLocalRatings(recensioniHotel, h); 
                double globalRate = makeGlobalRate(recensioniHotel, h);

                h.setRatings(ratings);
                h.setRate(globalRate);        
            }
        }

        db.saveHotels(hotels); //salviamo nel file la lista aggiornata

        //propaghiamo la lista aggiornata degli hotel ai rank

        List <LocalRank> localRanks = db.scanLocalRankings();
        List <LocalRank> daSalvare = new ArrayList<LocalRank>(); 

        for (LocalRank l : localRanks){

            List <LocalRankHotel> cityHotels = new ArrayList<LocalRankHotel>(); //gli hotel della città (denotata dal rank) 
            for (Hotel h : hotels){
                if (h.getCity().equals(l.getCittà())){
                    cityHotels.add(new LocalRankHotel(h, 0)); //la posizione degli hotel la ri-azzero in quanto va ricalcolata  
                }
            }

            if (cityHotels.size() > 0){
                daSalvare.add(new LocalRank(l.getCittà(), cityHotels));
            }
        }

        db.saveRankings(daSalvare);
    }

    /**
     * Questa funzione prende la lista dei ranking locali e, per ogni rank, controlla la lista delle recensioni e aggiorna gli hotel al suo interno
     * 
     * Per ogni città prendo il pool local rank (insieme degli hotel della città) e lo salvo. Da questo pool estraggo sempre l'hotel 'migliore' : globalRate più alto (calcolato tramite le recensioni) e lo tolgo dal pool per
     * metterlo in una lista ordinata (classifica aggiornata) da salvare 
     */
    public List<LocalRank> aggiornaRankingLocali (){ 
        List <String> cities = db.getAllCities(); //mi salvo le città
        List <LocalRank> localRanks = db.scanLocalRankings(); //rank attuali locali
        List <LocalRank> daSalvare = new ArrayList<LocalRank>(); 

        for (String c : cities){
            List<LocalRankHotel> cityRankHotels = new ArrayList<LocalRankHotel>();
            for (LocalRank l : localRanks){
                if (l.getCittà().equals(c)){
                    cityRankHotels = l.getListaLocalHotel(); 
                    break; 
                }
            }

            if (cityRankHotels != null && !cityRankHotels.isEmpty()){
                //ricalcolo il rank degli hotel della determinata città
                List<LocalRankHotel> nuovaListaRankHotel = new ArrayList<LocalRankHotel>();
                int i = 1; //indice per la posizione in classifica
                while (cityRankHotels.size() != 0){
                    LocalRankHotel daInserire = getBestHotel(cityRankHotels); //prendo sempre il migliore e lo classifico

                    if (daInserire != null){
                    daInserire.setRank(i++);
                    nuovaListaRankHotel.add(daInserire);

                    //usiamo un iteratore per rimuovere l'elemento dalla lista degli hotel della città
                    
                    Iterator<LocalRankHotel> iterator = cityRankHotels.iterator();
                        while (iterator.hasNext()) {
                            LocalRankHotel hotel = iterator.next();
                            if (hotel.equals(daInserire)) {
                                iterator.remove();
                                break;
                            }
                        }
                }
            }            
                daSalvare.add(new LocalRank(c, nuovaListaRankHotel)); //ranking locale aggiornato   
            }
        }

        //lo salvo sul file rankings.json
        db.saveRankings(daSalvare);
        return daSalvare; 
    }


    /**
     * 
     * @param recensioni
     * @return
     */
    public static Ratings makeLocalRatings (List<Recensione> recensioni, Hotel h){

        if (recensioni == null || recensioni.size() == 0) return h.getRatings();

        Ratings temp = new Ratings(0, 0, 0, 0); 
        Date today = new Date(); 
        double totalWeight = 0; //somma dei pesi

        for (Recensione r : recensioni){
            Ratings k = r.getLocalRates();

            long differenzaGiorni = (today.getTime() - r.getData().getTime()) / (1000 * 60 * 60 * 24);
            double tempWeight = Math.exp(-differenzaGiorni / 30.0);

            temp.setCleaning(temp.getCleaning() + (k.getCleaning()*tempWeight));
            temp.setPosition(temp.getPosition() + (k.getPosition()*tempWeight));
            temp.setQuality(temp.getQuality() + (k.getQuality()*tempWeight));
            temp.setServices(temp.getServices() + (k.getServices()*tempWeight));

            totalWeight += tempWeight; 
        }
 
        if (totalWeight > 0){       
        temp.setCleaning(temp.getCleaning()/ totalWeight);
        temp.setPosition(temp.getPosition()/ totalWeight);
        temp.setQuality(temp.getQuality()/ totalWeight);
        temp.setServices(temp.getServices()/ totalWeight);

        //formattazione ad una cifra decimale
        DecimalFormat df = new DecimalFormat("#.#");
        temp.setCleaning(Double.valueOf(df.format(temp.getCleaning())));
        temp.setPosition(Double.valueOf(df.format(temp.getPosition())));
        temp.setQuality(Double.valueOf(df.format(temp.getQuality())));
        temp.setServices(Double.valueOf(df.format(temp.getServices())));

        return temp; 
        }

        return h.getRatings();
    }

    public static double makeGlobalRate (List<Recensione> recensioni, Hotel h){

        if (recensioni == null || recensioni.size() == 0) return h.getRate(); 

        double aggiornato = 0; 
        Date today = new Date(); 
        double totalWeight = 0; //somma dei pesi

        for (Recensione r : recensioni){

            long differenzaGiorni = (today.getTime() - r.getData().getTime()) / (1000 * 60 * 60 * 24);
            double tempWeight = Math.exp(-differenzaGiorni / 30.0);
            aggiornato = aggiornato + (r.getGlobalRate() * tempWeight);
            totalWeight += tempWeight; 
        }

        if (totalWeight > 0) {
            double rate = aggiornato/recensioni.size();
            DecimalFormat df = new DecimalFormat("#.#");
            return Double.valueOf(df.format(rate));
        }

        return h.getRate();
        
    }

    public static LocalRankHotel getBestHotel (List <LocalRankHotel> listaHotels){

        if (listaHotels == null || listaHotels.isEmpty()) return null; 

        LocalRankHotel res = new LocalRankHotel(new Hotel(new Ratings(0, 0, 0, 0)), 0);

        for (LocalRankHotel h : listaHotels){
            
            if (h.getH().getRate() >= res.getH().getRate()){
                    res = h;         
            }
        }
        
        return res; 
    }

    /**
     * Funzione che crea l'insieme dei rank locali per ogni città. 
     * Inserisce dentro il file "Rankings.json" un'inizializzazione dei rank 
     */ 

    public void makeLocalRankings(){
        List<Hotel> listaHotels = db.scanHotels(); 
        List<String> listaCitta = db.getAllCities();
        List <LocalRank> daStampare = new ArrayList<LocalRank>(); 
        
        for (String city : listaCitta){
            LocalRank localRankCitta = new LocalRank(city); 
            for (Hotel h : listaHotels){
                if (h.getCity().equals(city)){
                    localRankCitta.addLocalHotel(new LocalRankHotel(h, 0));
                }
            }       
            daStampare.add(localRankCitta); 
        }
        
        //aggiungo la lista iniziale dei rank locali al file Rankings.json
        db.saveRankings(daStampare);
    }
}

