package hotelier.Server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import hotelier.Structures.Hotel;
import hotelier.Structures.LocalRank;
import hotelier.Structures.Recensione;
import hotelier.Structures.Utente;

public class JsonDB {
    private static JsonDB instance; //usiamo il pattern singleTon per il database
    
    private JsonDB(){} //viene chiamato solo da getInstance

    // Metodo statico per ottenere l'istanza del database
    public static synchronized JsonDB getInstance() {
        if (instance == null) {
            instance = new JsonDB();
        }
        return instance;
    }

    // converte il file JSON degli hotel in una struttura dati (che implementa List)
    // maneggevole
    public synchronized List<Hotel> scanHotels() {
        String hotelPath = "Hotels.json";
        List<Hotel> hotels = new ArrayList<Hotel>();// metterò gli hotel del file in questa struttura e la manderò in
                                                    // output
        try {
            Type hotelListType = new TypeToken<List<Hotel>>() {
            }.getType();
            hotels = new Gson().fromJson(new FileReader(hotelPath), hotelListType); // hotel deserializzati
        } catch (FileNotFoundException e) {
            System.err.println("I/O error while reading the file: " + hotelPath);
            e.printStackTrace();
        }

        return hotels;
    }

    // converte il file JSON degli utenti in una struttura dati (che implementa
    // List) maneggevole
    public synchronized List<Utente> scanUtenti() {
        String UtentiPath = "Utenti.json";
        List<Utente> utenti = new ArrayList<Utente>();
        try {
            Type utenteListType = new TypeToken<List<Utente>>() {
            }.getType();
            utenti = new Gson().fromJson(new FileReader(UtentiPath), utenteListType);
        } catch (FileNotFoundException e) {
            System.err.println("I/O error while reading the file: " + UtentiPath);
            e.printStackTrace();
        }
        return utenti;
    }

    // converte il file JSON delle recensioni in una struttura dati (che implementa
    // List) maneggevole
    public synchronized List<Recensione> scanRecensioni() {
        String recensioniPath = "Recensioni.json";
        List<Recensione> recensioni = new ArrayList<Recensione>();
        try {
            Type recensioneListType = new TypeToken<List<Recensione>>() {
            }.getType();
            recensioni = new Gson().fromJson(new FileReader(recensioniPath), recensioneListType);
        } catch (FileNotFoundException e) {
            System.err.println("I/O error while reading the file: " + recensioniPath);
            e.printStackTrace();
        }
        return recensioni;
    }

    /**
     * Funzione che restituisce l'insieme dei rank locali per ogni città. 
     * @return LocalRankList, Lista che contiene i rank locali per ogni città
     */
    public synchronized List<LocalRank> scanLocalRankings(){
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

    //salva i dati dell'utente (identificato tramite l'oggetto) nel file utenti.json
    public synchronized void saveUtente (Utente aggiornato){
        List<Utente> listaUtenti = scanUtenti(); 
        if (listaUtenti !=  null){
            for (int i = 0; i < listaUtenti.size(); i++){
                Utente u = listaUtenti.get(i);
                if (u.getUsername().equals(aggiornato.getUsername())){
                    synchronized(this){
                        listaUtenti.set(i, aggiornato);
                    }
                }
            }
            
            //risalvo la lista
        File inputUtenti = new File(
                "Utenti.json");
        try (FileWriter writer = new FileWriter(inputUtenti)) {
            new Gson().toJson(listaUtenti, writer);
        } catch (IOException e) {
            e.printStackTrace();
            }
        }  
    }

    public synchronized void saveRecensioni (List<Recensione> listaRecensioni){
        File input = new File("Recensioni.json");
                        try (FileWriter writer = new FileWriter(input)) {
                            synchronized(this){
                                new Gson().toJson(listaRecensioni, writer);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
    }

    public synchronized void saveHotels (List<Hotel> listaHotels){

        File input = new File(
                "Hotels.json");
        try (FileWriter writer = new FileWriter(input)) {
            new Gson().toJson(listaHotels, writer);
        } catch (IOException e) {
            e.printStackTrace();
            }
        }
    

    public synchronized  void saveRankings (List <LocalRank> listaRank){
        File input = new File(
            "Rankings.json");
            try (FileWriter writer = new FileWriter(input)) {
                new Gson().toJson(listaRank, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    public synchronized List<String> getAllCities (){
        List <String> res = new ArrayList<String>(); 
            List<Hotel> listaHotels = instance != null ? instance.scanHotels() : null; 
            if (listaHotels == null) return null; 
            
            for (Hotel h : listaHotels){
                if (!res.contains(h.getCity())){
                    res.add(h.getCity());
                }
            }
    
            return res; 
        }

}