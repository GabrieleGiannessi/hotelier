package hotelier;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

//questo task implementa la sessione tra client e server
public class Sessione implements Runnable {

    private Socket s;
    private MulticastSocket m; 
    private String group; 
    private int port; 
    private JsonDB db; 

    public Sessione(Socket s, MulticastSocket m, String group, int port, JsonDB db) {
        this.s = s;
        this.m = m; 
        this.group = group; 
        this.port = port; 
        this.db = db; 
    }

    @Override
    public void run() {

        Utente user = null; //tengo traccia dell'utente che si autentica

        try (ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            
            boolean exit = false;

            while (!exit) {
                int op=9;
                try{
                     op = in.readInt();
                }catch (IOException e){
                    //interruzione da parte del client
                    exit = true; 
                }
                switch (op) {
                    case 1: // registrazione
                    {
                        if (user != null && user.isLogged()) {
                            out.writeInt(0);
                            out.flush();
                            break;
                        } else {
                            out.writeInt(1);
                            out.flush();
                        }

                        // ricevo l'utente in input
                        Utente utente = (Utente) in.readObject();

                        if (!checkUsername(utente.getUsername()) && !checkPassword(utente.getPassword())) { 
                            out.writeInt(2); //errore sintassi credenziali utente
                            out.writeUTF("Credenziali non corrette (Lo username deve contenere almeno 5 caratteri, e la password almeno 8)"); // mandiamo il badge
                            out.flush();
                            break;
                        }
                    
                        else if (!checkUsername(utente.getUsername())) {
                            out.writeInt(2); //errore sintassi credenziali utente
                            out.writeUTF("Username non corretto (Lo username deve contenere almeno 5 caratteri)"); // mandiamo il badge
                            out.flush();    
                            break;

                        } else if (!checkPassword(utente.getPassword())) {
                            out.writeInt(2); //errore sintassi credenziali utente
                            out.writeUTF("Password non corretta (La password deve contenere almeno 8 caratteri e non deve contenere spazi)"); // mandiamo il badge
                            out.flush();
                            break; 

                        }

                        List<Utente> listaUtenti = db.scanUtenti();
                        if (listaUtenti == null) {
                            // inizializzo la lista
                            listaUtenti = new ArrayList<Utente>();
                        } else {
                            // controllo se l'username è presente tra gli utenti registrati
                            boolean flag = false; // flag che indica se l'username è già presente
                            for (Utente u : listaUtenti) {
                                if (u.getUsername().equals(utente.getUsername())) {
                                    flag = true;
                                    break; // esco dal ciclo
                                }
                            }

                            if (flag) {
                                // notifico il client che l'username è già presente: username deve essere
                                // univoco
                                out.writeInt(0);
                                out.flush();
                                break;
                            }
                        }

                        // inserisco l'utente nel file json
                        listaUtenti.add(utente); // inserisco l'utente

                        // re - inserisco la lista di utenti nel file JSON
                        File inputUtenti = new File(
                                "Utenti.json");
                        try (FileWriter writer = new FileWriter(inputUtenti)) {
                            new Gson().toJson(listaUtenti, writer);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        out.writeInt(1); //la registrazione ha avuto successo 
                        out.flush();
                        break;
                    }

                    case 2: // login
                    {

                        // controllo se il client è già loggato
                        if (user != null && user.isLogged()) {
                            out.writeInt(0); // non ci si può autenticare se siamo già autenticati: si deve effettuare
                                             // logout
                            out.flush();
                            break;
                        } else {
                            // il client può autenticarsi
                            out.writeInt(1);
                            out.flush();
                        }

                        Utente inputUser = (Utente) in.readObject(); // ricevo l'utente in input

                        if (!checkUsername(inputUser.getUsername()) && !checkPassword(inputUser.getPassword())) {
                            out.writeInt(2); //errore sintassi credenziali utente
                            out.writeUTF("Credenziali non corrette (Lo username deve contenere almeno 5 caratteri, e la password almeno 8)"); 
                            out.flush();
                            break;
                        }
                
                        else if (!checkUsername(inputUser.getUsername())) {
                            out.writeInt(2); //errore sintassi credenziali utente
                            out.writeUTF("Username non corretto (Lo username deve contenere almeno 5 caratteri)"); 
                            out.flush();    
                            break;

                        } else if (!checkPassword(inputUser.getPassword())) {
                            out.writeInt(2); //errore sintassi credenziali utente
                            out.writeUTF("Password non corretta (La password deve contenere almeno 8 caratteri e non deve contenere spazi)"); 
                            out.flush();
                            break; 
                        }

                        //se la sintassi di username e password è corretta procediamo con i controlli successivi
                
                        List<Utente> listaUtenti = db.scanUtenti(); // prelevo la lista degli utenti dal file
                        if (listaUtenti == null) {
                            out.writeInt(0); // operazione fallita
                            out.flush();
                            break;
                        }

                        for (Utente u : listaUtenti) {
                            if (u.getUsername().equals(inputUser.getUsername())) {
                                if (u.getPassword().equals(inputUser.getPassword())) {
                                    // l'utente si è autenticato
                                    if (!u.isLogged()){
                                    user = u;
                                    user.setLogged(true);
                                    break;
                                    }
                                    
                                } else
                                    break; // altrimenti password errata
                            }
                        }

                        if (user != null && user.isLogged()) {

                            db.saveUtente(user); //salvo il fatto che sia loggato
                            out.writeInt(1);
                            out.writeObject(user); // operazione riuscita: l'utente si è autenticato
                            out.flush();
                        } else {
                            out.writeInt(0); // operazione fallita
                            out.flush();
                        }
                        break;
                    }

                    case 3: // logout
                    {
                        if ((user != null && !user.isLogged()) || user == null) {
                            out.writeInt(0); //operazione fallita
                            out.flush();
                            break;
                        }

                        user.setLogged(false);       
                        db.saveUtente(user); //salvataggio dell'Utente nel file (Utenti.json)
                        user = null; 

                        sendUDPmessage(m); //messaggio inviato per far terminare il task notifiche

                        out.writeInt(1); // operazione effettuata
                        out.flush();
                        break;
                    }

                    case 4: // searchHotel
                    {
                        List<Hotel> listaHotel = db.scanHotels();
                        String nomeHotel = in.readUTF();

                        if (nomeHotel.length() == 0) break;

                        boolean f_nome = false;

                        // controllo nome
                        for (Hotel h : listaHotel) {
                            if (h.getName().equals(nomeHotel)) {
                                f_nome = true;
                                break;
                            }
                        }

                        if (f_nome) {
                            out.writeInt(1);
                            out.flush();
                        } else {
                            out.writeInt(0); // messaggio di errore nomeHotel
                            out.flush();
                            break; 
                        }

                        // controllo città
                        String città = in.readUTF();

                        if (città == null){
                            out.writeInt(2); //errore sintassi della stringa 
                            out.writeUTF("Nome città vuoto"); // mandiamo il messaggio di errore
                            out.flush();    
                            break;
                        }

                        boolean f_città = false;
                        Hotel searchedHotel = null;

                        for (Hotel h : listaHotel) {
                            if (h.getName().equals(nomeHotel)) {
                                if (h.getCity().equals(città)) {
                                    searchedHotel = h;
                                    f_città = true;
                                    break;
                                }
                            }
                        }

                        if (f_città) {
                            out.writeInt(1);
                            out.flush();
                        } else {
                            out.writeInt(0); // messaggio di errore città
                            out.flush();
                            break; 
                        }

                        if (f_nome && f_città) {
                            //calcolo il numero di recensioni associate all'Hotel
                            List <Recensione> recensioni = db.scanRecensioni(); 
                            int c = 0; 
                            for (Recensione r : recensioni){
                                if (r.getNomeHotel().equals(nomeHotel)) c++; 
                            }

                            out.writeObject(searchedHotel);
                            out.writeInt(c); //numero di recensioni
                            out.flush();
                        }

                        break;
                    }

                    case 5: // searchAllHotels
                    {
                        List<LocalRank> listaRanks = db.scanLocalRankings();
                        String città = in.readUTF(); // ricevo il nome della città ed effettuo la query

                        if (città.length() == 0) break;
                        
                        boolean presente = false;
                        List<Hotel> daStampare = new ArrayList<Hotel>();

                        for (LocalRank l: listaRanks) {
                            if (l.getCittà().equals(città)) {
                                presente = true;
                                for (LocalRankHotel h : l.getListaLocalHotel()){
                                    daStampare.add(h.getH()); 
                                }
                                break; 
                            }
                        }

                        if (presente) {
                            out.writeInt(1); // messaggio di conferma
                            out.flush();

                            for (Hotel h : daStampare){
                                //calcolo il numero di recensioni associate all'hotel h
                                List <Recensione> recensioniHotel = db.scanRecensioni(); 
                                int c = 0;
                                for (Recensione r: recensioniHotel){
                                    if (r.getNomeHotel().equals(h.getName())) c++; 
                                }

                                out.writeObject(h); // hotel
                                out.writeInt(c);
                                out.flush();
                            }

                            out.writeObject(null); //gli hotel sono finiti 
                            out.flush();

                        } else {
                            out.writeInt(0);
                            out.flush();
                            break;
                        }
                        break;
                    }

                    case 6: //insertReview : inserisce una recensione all'interno del file .json
                    {
                        if ((user != null && !user.isLogged()) || user == null){
                            out.writeInt(0); //operazione fallita
                            out.flush();
                            break;
                        }

                        out.writeInt(1); //il cliente può accedere all'operazione
                        out.flush();

                        //gestione searchHotel : cerchiamo l'hotel che viene riferito dalla recensione, se lo troviamo facciamo compilare i voti, altrimenti segnaliamo errore

                        List<Hotel> listaHotel = db.scanHotels();
                        String nomeHotel = in.readUTF(); 

                        if (nomeHotel.length() == 0) {
                            //out.writeInt(0); // messaggio di errore 
                            //out.flush();
                            break;
                        }

                        boolean f_nome = false;

                        // controllo nome
                        for (Hotel h : listaHotel) {
                            if (h.getName().equals(nomeHotel)) {
                                f_nome = true;
                                break;
                            }
                        }

                        if (f_nome) {
                            out.writeInt(1);
                            out.flush();
                        } else {
                            out.writeInt(0); // messaggio di errore nomeHotel
                            out.flush();
                            break; 
                        }

                        // controllo città
                        String città = in.readUTF();

                        if (città.length() == 0) {
                            //out.writeInt(0); // messaggio di errore 
                            //out.flush();
                            break;
                        }

                        boolean f_città = false;
                        Hotel searchedHotel = null;

                        for (Hotel h : listaHotel) {
                            if (h.getName().equals(nomeHotel)) {
                                if (h.getCity().equals(città)) {
                                    searchedHotel = h;
                                    f_città = true;
                                    break;
                                }
                            }
                        }

                        if (f_città) {
                            out.writeInt(1); //città trovata
                            out.flush();
                        } else {
                            out.writeInt(0); // messaggio di errore città
                            out.flush();
                            break; 
                        }

                        if (f_nome && f_città) {
                            out.writeObject(searchedHotel);
                            out.flush();
                        }    
                      
                        Recensione review = null; 
                        try{
                            review = (Recensione) in.readObject();
                        }catch (IOException e){
                            System.err.println("Errore durante il ricevimento della recensione");
                        }

                        if (review == null) break; 

                        List<Recensione> listaRecensioni = db.scanRecensioni(); 
                        if (listaRecensioni == null){
                            listaRecensioni = new ArrayList<Recensione>();
                        }

                        boolean check = true; //controllo sui voti

                        //serie di controlli sui voti
                        if (review.getGlobalRate() > 5 || review.getGlobalRate() < 0) check = false;

                        Ratings r = review.getLocalRates();
                            
                        if (r.getCleaning() > 5 || r.getServices() > 5 || r.getQuality() > 5
                                || r.getPosition() > 5)
                            check = false;
                        if (r.getCleaning() < 0 || r.getServices() < 0 || r.getQuality() < 0
                                || r.getPosition() < 0)
                            check = false;

                        if (!check) {
                            out.writeInt(0); //messaggio di errore : errore sui campi
                            out.flush();
                            break;
                        }

                        listaRecensioni.add(review); //se la recensione supera i controlli sulla semantica la aggiungo al file json 
                        
                        //aggiornamento badge Utente
                        user.setNumeroRecensioni(user.getNumeroRecensioni() + 1);
                        if (user.getNumeroRecensioni() >= 5 && user.getNumeroRecensioni() < 10) user.setBadge("Recensore esperto");
                        if (user.getNumeroRecensioni() >= 10 && user.getNumeroRecensioni() < 20) user.setBadge("Contributore");
                        if (user.getNumeroRecensioni() >= 20 && user.getNumeroRecensioni() < 50) user.setBadge("Contributore esperto");
                        if (user.getNumeroRecensioni() >= 50) user.setBadge("Contributore super");

                        db.saveUtente(user); //inserisco i dati aggiornati
                        db.saveRecensioni(listaRecensioni); //re-inserisco la lista nel file

                        out.writeInt(1); //operazione conclusa
                        out.flush();
                        break;
                    }

                    case 7: // showBadge
                    {
                        if ((user != null && !user.isLogged()) || user == null) {
                            out.writeInt(0); // non si può visualizzare il distintivo
                            out.flush();
                            break;
                        }

                        // mandiamo il badge
                        out.writeInt(1);
                        out.writeUTF(user.getBadge()); 
                        out.flush();

                        break;
                    }

                    case 8: // Exit : non occorre mandare nessun messaggio al client, chiudo la connessione
                            // lato sessione
                    {
                        if (user != null && user.isLogged()){
                            sendUDPmessage(m); //per terminare il thread notifiche nel client
                            user.setLogged(false); 
                            db.saveUtente(user);
                            user = null;
                        } 
                        exit = true;
                        System.out.println("Client disconnesso");
                        break;
                    }

                    case 9: { //uscita speciale : il client ha richiesto la chiusura della connessione tramite interruzione (es : CTRL+C)
                        if (user != null && user.isLogged()){
                            //non invio nessun messaggio UDP in quanto il thread client è terminato (causerebbe broken pipe)
                            user.setLogged(false); 
                            db.saveUtente(user);
                            user = null;
                        }
                        exit = true;
                        System.out.println("Client disconnesso");
                        break;
                    }

                    default:
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (ClassNotFoundException c) {
            System.err.println(c.getMessage());
        } catch (NumberFormatException n){
            System.err.println("Immetti un numero!");
            System.err.println(n.getMessage());
        }
    }

    /**
     * Funzione usata per controllare l'input (username) del client
     * 
     * @param username , ovvero il nome con cui l'utente è registrato al servizio
     * @return un booleano che indica se l'username è semanticamente corretto
     */

    public static boolean checkUsername(String username) {
        return (username != null && !username.contains(" ") && username.length() >= 5);
    }

    /**
     * Funzione usata per controllare l'input (username) del client
     * 
     * @param password , ovvero il nome con cui l'utente è registrato al servizio
     * @return un booleano che indica se l'username è semanticamente corretto
     */
    public static boolean checkPassword(String pass) {
        return pass != null && !pass.contains(" ") && pass.length() >= 8;
    }

    public void sendUDPmessage (MulticastSocket m){
            try{
                String returnmess = "[FINE]";
                DatagramPacket dp = new DatagramPacket(returnmess.getBytes(), returnmess.length(), InetAddress.getByName(group), port);
                m.send(dp);
            }catch (IOException e){
                e.printStackTrace();
            }
    }
}
