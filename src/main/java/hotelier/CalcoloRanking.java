package hotelier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Task che esegue il calcolo del ranking e si occupa di mandare un messaggio UDP broadcast agli utenti loggati  
 */

public class CalcoloRanking implements Runnable {

    //ho bisogno di caricare la lista delle recensioni, e memorizzare in una struttura l'insieme dei voti complessivo per un determinato hotel (Potrei istanziare un oggetto Recensione)
    //attraverso queste strutture vado ad aggiornare la lista degli hotel

    /**
     * Il rank locale di un hotel viene calcolato in base alla posizione di quell’hotel rispetto a tutti gli altri hotel della stessa città.
     * Come posso aggiornare i ranking degli hotel?
     * 1. Prelevare l'insieme delle recensioni e degli hotel
     * 2. Per ogni hotel, salvare le recensioni che riguardano il determinato hotel (doppio for)
     * 3. In questi insiemi (dopo il secondo for ma dentro il primo) farò il calcolo vero e proprio : conterò qualità e quantità delle recensioni per ogni hotel
     * 4. effettuato il calcolo lo salvo in Hotel
     * 5. Finito il secondo for, salvo i risultati nel file
     *  */ 

    private MulticastSocket m; 
    private String group;
    private int port; 

    public CalcoloRanking (MulticastSocket m, String group, int port){
        this.m = m; 
        this.group = group;
        this.port = port; 
    }

    @Override
    public void run() {
         
        List <LocalRank> before = scanLocalRankings();
        if (before == null){
            makeLocalRankings();
            before = scanLocalRankings();
        } 
        
        aggiornaRatingHotels(); //si aggiornano i rates (voti) degli hotel
        List <LocalRank> after = aggiornaRankingLocali(); //ricalcolo le posizioni degli hotel in base alle recensioni (del momento)

         /* 
        for (Hotel i : after){
            boolean flag = false; //flag per capire se il local ranking è cambiato per la determinata città
            for (Hotel j : before){
                if (i.getCity().equals(j.getCity())){
                    if (!i.getName().equals(j.getName())){
                        flag = true; 
                        break; 
                    }
                }
            }

            
            if (flag){ //se il ranking è cambiato mandiamo il messaggio
                try{
                    
                String mess = "Ranking locale cambiato per "+ i.getCity()+ " : "+i.getName()+ " \n";
                DatagramPacket dp = new DatagramPacket(mess.getBytes(), mess.length(), InetAddress.getByName(group), port);
                m.send(dp);
                
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
*/              
    }

    /**
     * Attraverso questa funzione vengono aggiornati i voti (sia globali che più specifici) riferiti agli hotel, in cui vengono considerate anche le date in cui le recensioni
     * sono state fatte 
     */

    public static void aggiornaRatingHotels (){
        List <Hotel> hotels = scanHotels(); 
        List <Recensione> recensioni = scanRecensioni(); 

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

        saveHotels(hotels); //salviamo nel file la lista aggiornata

        //propaghiamo la lista aggiornata degli hotel ai rank

        List <LocalRank> localRanks = scanLocalRankings();
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

        saveRankings(daSalvare);
    }

    /**
     * Questa funzione prende la lista dei ranking locali e, per ogni rank, controlla la lista delle recensioni e aggiorna gli hotel al suo interno
     * 
     * Per ogni città prendo il pool local rank (insieme degli hotel della città) e lo salvo. Da questo pool estraggo sempre l'hotel 'migliore' : globalRate più alto (calcolato tramite le recensioni) e lo tolgo dal pool per
     * metterlo in una lista ordinata (classifica aggiornata) da salvare 
     */
    public static List<LocalRank> aggiornaRankingLocali (){ 
        List <String> cities = getAllCities(); //mi salvo le città
        List <LocalRank> localRanks = scanLocalRankings(); //rank attuali locali
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
                List<LocalRankHotel> nuovoRank = new ArrayList<LocalRankHotel>();
                int i = 1; //indice per la posizione in classifica
                while (cityRankHotels.size() != 0){
                    LocalRankHotel daInserire = getBestHotel(cityRankHotels); //prendo sempre il migliore e lo classifico
                    System.out.println(daInserire.getH().getName());
                    daInserire.setRank(i++);
                    nuovoRank.add(daInserire);

                    //usiamo un iteratore per rimuovere l'elemento dalla lista degli hotel della città
                    //System.out.println(cityRankHotels.size());
                    Iterator<LocalRankHotel> iterator = cityRankHotels.iterator();
                        while (iterator.hasNext()) {
                            LocalRankHotel hotel = iterator.next();
                            if (hotel.equals(daInserire)) {
                                iterator.remove();
                                break;
                            }
                        }
                   // System.out.println(cityRankHotels.size());
                }
                daSalvare.add(new LocalRank(c, nuovoRank)); //ranking locale aggiornato
            }
        }

        //lo salvo sul file rankings.json
        saveRankings(daSalvare);

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

    public static List<String> getAllCities (){
        List <String> res = new ArrayList<String>(); 
        List<Hotel> listaHotels = scanHotels(); 
        for (Hotel h : listaHotels){
            if (!res.contains(h.getCity())){
                res.add(h.getCity());
            }
        }

        return res; 
    }

    public static LocalRankHotel getBestHotel (List <LocalRankHotel> listaHotels){

        LocalRankHotel res = new LocalRankHotel(new Hotel(new Ratings(0, 0, 0, 0)), 0);

        for (LocalRankHotel h : listaHotels){
            
            if (h.getH().getRate() > res.getH().getRate()){
                if (h.getH().getRatings().getCleaning() > res.getH().getRatings().getCleaning() && h.getH().getRatings().getPosition() > res.getH().getRatings().getPosition() && 
                h.getH().getRatings().getQuality() > res.getH().getRatings().getQuality() && h.getH().getRatings().getServices() > res.getH().getRatings().getServices()){
                    res = h; 
                }
            }
        }
        
        return res; 
    }

    /**
     * Funzione che crea l'insieme dei rank locali per ogni città. 
     * Inserisce dentro il file "Rankings.json" un'inizializzazione dei rank 
     */ 

    public static void makeLocalRankings(){
        List<Hotel> listaHotels = scanHotels(); 
        List<String> listaCitta = getAllCities();
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
        saveRankings(daStampare);
    }

    /**
     * Funzione che restituisce l'insieme dei rank locali per ogni città. 
     * @return LocalRankList, Lista che contiene i rank locali per ogni città
     */

    public synchronized static List<LocalRank> scanLocalRankings(){
        String rankingPath = "Rankings.json";    
        List<LocalRank> localRankList = new ArrayList<LocalRank>();
        try {
            Type localRankListType = new TypeToken<List<LocalRank>>() {
            }.getType();
            localRankList = new Gson().fromJson(new FileReader(rankingPath), localRankListType);
        } catch (FileNotFoundException e) {
            System.err.println("I/O error while reading the file: " + rankingPath);
            e.printStackTrace();
        }
        return localRankList;
    }

    
    public synchronized static List<Hotel> scanHotels() {
        String hotelPath = "Hotels.json";
        List<Hotel> hotels = new ArrayList<Hotel>();
        try {
            Type hotelListType = new TypeToken<List<Hotel>>() {
            }.getType();
            hotels = new Gson().fromJson(new FileReader(hotelPath), hotelListType); // hotel deserializzati
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return hotels;
    }

    public synchronized static List<Recensione> scanRecensioni() {
        String recensioniPath = "Recensioni.json";
        List<Recensione> recensioni = new ArrayList<Recensione>();
        try {
            Type recensioneListType = new TypeToken<List<Recensione>>() {
            }.getType();
            recensioni = new Gson().fromJson(new FileReader(recensioniPath), recensioneListType);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return recensioni;
    }

    public synchronized static void saveHotels (List<Hotel> listaHotels){

        File input = new File(
                "Hotels.json");
        try (FileWriter writer = new FileWriter(input)) {
            new Gson().toJson(listaHotels, writer);
        } catch (IOException e) {
            e.printStackTrace();
            }
        }
    

    public synchronized static void saveRankings (List <LocalRank> listaRank){
        File input = new File(
            "Rankings.json");
            try (FileWriter writer = new FileWriter(input)) {
                new Gson().toJson(listaRank, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

