package hotelier.Server;

import com.google.gson.Gson;

import hotelier.Messages.ClientMessages.*;
import hotelier.Messages.ServerMessages.*;
import hotelier.Structures.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;



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

        Utente user = null; //istanzio questo oggetto per tenere traccia dell'utente che si autentica

        try (ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            
            boolean exit = false;

            while (!exit) {
                ClientMessage op = new ControlClientMessage(9); 
                try{
                     op = (ClientMessage) in.readObject(); //mi metto in ascolto del messaggio dal client

                }catch (IOException e){
                    //interruzione da parte del client
                    exit = true; 
                }
                if (op instanceof ControlClientMessage){
                    ControlClientMessage scelta = (ControlClientMessage) op; 
                    switch (scelta.getCod()) {
                        case 1: // registrazione
                        {
                            if (user != null && user.isLogged()) {
                                out.writeObject(new ControlServerMessage(0));
                                out.flush();
                                break;
                            } else {
                                out.writeObject(new ControlServerMessage(1));
                                out.flush();
                            }
    
                            // ricevo l'utente in input
                            ClientMessage mess = (ClientMessage) in.readObject();

                            if (mess instanceof ControlClientMessage){
                                ControlClientMessage cod = (ControlClientMessage) mess; 
                                if (cod.getCod() == 9){
                                    exit = true;
                                    System.out.println("Client disconnesso");
                                    break;
                                }
                            }
    
                            if (mess instanceof UserClientMessage){
                            UserClientMessage utente =(UserClientMessage) mess; 
    
                            if (!checkUsername(utente.getUser().getUsername()) && !checkPassword(utente.getUser().getPassword())) { 
                                out.writeObject(new ControlServerMessage(2));
                                out.writeObject(new StringServerMessage("Credenziali non corrette (Lo username deve contenere almeno 5 caratteri, e la password almeno 8)")); // mandiamo il badge
                                out.flush();
                                break;
                            }
                        
                            else if (!checkUsername(utente.getUser().getUsername())) {
                                out.writeObject(new ControlServerMessage(2));
                                out.writeObject(new StringServerMessage("Username non corretto (Lo username deve contenere almeno 5 caratteri)"));
                                out.flush();    
                                break;
    
                            } else if (!checkPassword(utente.getUser().getPassword())) {
                                out.writeObject(new ControlServerMessage(2));
                                out.writeObject(new StringServerMessage("Password non corretta (La password deve contenere almeno 8 caratteri e non deve contenere spazi)"));
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
                                    if (u.getUsername().equals(utente.getUser().getUsername())) {
                                        flag = true;
                                        break; // esco dal ciclo
                                    }
                                }
    
                                if (flag) {
                                    // notifico il client che l'username è già presente: username deve essere
                                    // univoco
                                    out.writeObject(new ControlServerMessage(0));
                                    out.flush();
                                    break;
                                }
                            }
    
                            //prima di inserire l'utente nel db devo convertire la password in chiaro in una password cifrata (usando un algoritmo di cifratura: uso message digest MD5 che genera una stringa di 32 caratteri)
                            String encryptedPassword = null; 
                            try {
                                MessageDigest m = MessageDigest.getInstance("MD5");
                                m.update(utente.getUser().getPassword().getBytes());
                                byte[] bytes = m.digest();  
                                StringBuilder s = new StringBuilder();  
                                for(int i=0; i< bytes.length ;i++)  //conversione dei byte nell'array in versione esadecimale
                                {  
                                    s.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));  
                                }

                                encryptedPassword = s.toString();
                                utente.getUser().setPassword(encryptedPassword); //setto la pass cifrata

                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            }

                            // inserisco l'utente nel file json
                            listaUtenti.add(utente.getUser()); // inserisco l'utente
    
                            // re - inserisco la lista di utenti nel file JSON
                            File inputUtenti = new File(
                                    "Utenti.json");
                            try (FileWriter writer = new FileWriter(inputUtenti)) {
                                new Gson().toJson(listaUtenti, writer);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
    
                            out.writeObject(new ControlServerMessage(1)); //la registrazione ha avuto successo 
                            out.flush();
                            }
    
                            break;
                        }
    
                        case 2: // login
                        {
    
                            // controllo se il client è già loggato
                            if (user != null && user.isLogged()) {
                                out.writeObject(new ControlServerMessage(0)); // non ci si può autenticare se siamo già autenticati: si deve effettuare
                                                 // logout
                                out.flush();
                                break;
                            } else {
                                // il client può autenticarsi
                                out.writeObject(new ControlServerMessage(1));
                                out.flush();
                            }
    
                            ClientMessage clientMessage = (ClientMessage) in.readObject(); // ricevo l'utente in input

                            if (clientMessage instanceof ControlClientMessage){
                                ControlClientMessage cod = (ControlClientMessage) clientMessage; 
                                if (cod.getCod() == 9){
                                    exit = true;
                                    System.out.println("Client disconnesso");
                                    break;
                                }
                            }

                            if (clientMessage instanceof UserClientMessage){
                                UserClientMessage userMessage = (UserClientMessage) clientMessage; 
                                Utente inputUser = userMessage.getUser(); 

                                if (!checkUsername(inputUser.getUsername()) && !checkPassword(inputUser.getPassword())) {
                                    out.writeObject(new ControlServerMessage(2)); //errore sintassi credenziali utente
                                    out.writeObject(new StringServerMessage("Credenziali non corrette (Lo username deve contenere almeno 5 caratteri, e la password almeno 8)")); 
                                    out.flush();
                                    break;
                                }
                        
                                else if (!checkUsername(inputUser.getUsername())) {
                                    out.writeObject(new ControlServerMessage(2)); //errore sintassi credenziali utente
                                    out.writeObject(new StringServerMessage("Username non corretto (Lo username deve contenere almeno 5 caratteri)"));
                                    out.flush();    
                                    break;
        
                                } else if (!checkPassword(inputUser.getPassword())) {
                                    out.writeObject(new ControlServerMessage(2)); //errore sintassi credenziali utente
                                    out.writeObject(new StringServerMessage("Password non corretta (La password deve contenere almeno 8 caratteri e non deve contenere spazi)"));
                                    out.flush();
                                    break; 
                                }
        
                                //se la sintassi di username e password è corretta procediamo con i controlli successivi
                        
                                List<Utente> listaUtenti = db.scanUtenti(); // prelevo la lista degli utenti dal file
                                if (listaUtenti == null) {
                                    out.writeObject(new ControlServerMessage(0)); // operazione fallita
                                    out.flush();
                                    break;
                                }

                                //controllo se la password è corretta cifrandola (la pass cifrata verrà confrontata con quelle presenti nel db) 

                                String encryptedPassword = null; 
                            try {
                                MessageDigest m = MessageDigest.getInstance("MD5");
                                m.update(inputUser.getPassword().getBytes());
                                byte[] bytes = m.digest();  
                                StringBuilder s = new StringBuilder();  
                                for(int i=0; i< bytes.length ;i++)  //conversione dei byte nell'array in versione esadecimale
                                {  
                                    s.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));  
                                }

                                encryptedPassword = s.toString();
                                inputUser.setPassword(encryptedPassword); //setto la pass cifrata

                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
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
                                    out.writeObject(new ControlServerMessage(1));
                                    out.writeObject(new UserServerMessage(user)); // operazione riuscita: l'utente si è autenticato
                                    out.flush();
                                } else {
                                    out.writeObject(new ControlServerMessage(0)); // operazione fallita
                                    out.flush();
                                }
                            }
    
                            break;
                        }
    
                        case 3: // logout
                        {
                            if ((user != null && !user.isLogged()) || user == null) {
                                out.writeObject(new ControlServerMessage(0));; //operazione fallita
                                out.flush();
                                break;
                            }
    
                            user.setLogged(false);       
                            db.saveUtente(user); //salvataggio dell'Utente nel file (Utenti.json)
                            user = null; 
    
                            sendUDPmessage(m); //messaggio inviato per far terminare il task notifiche
    
                            out.writeObject(new ControlServerMessage(1));; // operazione effettuata
                            out.flush();
                            break;
                        }
    
                        case 4: // searchHotel
                        {
                            List<Hotel> listaHotel = db.scanHotels();
                            ClientMessage res;
                            while ((res = (ClientMessage) in.readObject()) == null){} 
    
                            if (res instanceof ControlClientMessage){
                                ControlClientMessage cod = (ControlClientMessage) res; 
                                if (cod.getCod() == 9){
                                    //chiudi la sessione (cod. 9)
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
                            }
                            
                            if (res instanceof StringClientMessage){
                                StringClientMessage nomeHotelMessage = (StringClientMessage) res; 
                                String nomeHotel = nomeHotelMessage.getMess(); 
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
                                out.writeObject(new ControlServerMessage(1));
                                out.flush();
                            } else {
                                out.writeObject(new ControlServerMessage(0)); // messaggio di errore nomeHotel
                                out.flush();
                                break; 
                            }
    
                            ClientMessage citymess;
                            while ((citymess = (ClientMessage) in.readObject()) == null){} 

                            if (citymess instanceof ControlClientMessage){
                                ControlClientMessage cod = (ControlClientMessage) citymess; 
                                if (cod.getCod() == 9){
                                    //chiudi la sessione (cod. 9)
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
                            }

                            if (citymess instanceof StringClientMessage){
                                StringClientMessage cityMessage = (StringClientMessage) citymess; 
                                String città = cityMessage.getMess(); 

                                if (città.length() == 0) break;

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
                                out.writeObject(new ControlServerMessage(1));;
                                out.flush();
                                } else {
                                out.writeObject(new ControlServerMessage(0)); // messaggio di errore città
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
    
                                out.writeObject(new HotelServerMessage(searchedHotel));
                                out.writeInt(c); //numero di recensioni
                                out.flush();
                                }
                            }       
                        }
                            break;
                        }
    
                        case 5: // searchAllHotels
                        {
                            //faccio la scansione dei rank in quanto ho bisogno di ordinare gli hotel per le posizioni che hanno sul rank locale della città
                            List<LocalRank> listaRanks = db.scanLocalRankings(); 

                            ClientMessage res; 
                            while ((res = (ClientMessage) in.readObject()) == null){}

                            if (res instanceof ControlClientMessage){
                                ControlClientMessage cod = (ControlClientMessage) res; 
                                if (cod.getCod() == 9){
                                    //chiudi la sessione (cod. 9)
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
                            }

                            if (res instanceof StringClientMessage){
                                StringClientMessage cityMessage = (StringClientMessage) res; 
                                String città = cityMessage.getMess(); 

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
                                    out.writeObject(new ControlServerMessage(1)); // messaggio di conferma
                                    out.flush();
        
                                    for (Hotel h : daStampare){
                                        //calcolo il numero di recensioni associate all'hotel h
                                        List <Recensione> recensioniHotel = db.scanRecensioni(); 
                                        int c = 0;
                                        for (Recensione r: recensioniHotel){
                                            if (r.getNomeHotel().equals(h.getName())) c++; 
                                        }
        
                                        out.writeObject(new HotelServerMessage(h)); // hotel
                                        out.writeInt(c); //numero recensioni
                                        out.flush();
                                    }
        
                                    out.writeObject(null); //gli hotel sono finiti 
                                    out.flush();
        
                                } else {
                                    out.writeObject(new ControlServerMessage(0));
                                    out.flush();
                                    break;
                                }
                            }

                            
                            break;
                        }
    
                        case 6: //insertReview : inserisce una recensione all'interno del file .json
                        {
                            if ((user != null && !user.isLogged()) || user == null){
                                out.writeObject(new ControlServerMessage(0)); //operazione fallita
                                out.flush();
                                break;
                            }
    
                            out.writeObject(new ControlServerMessage(1)); //il cliente può accedere all'operazione
                            out.flush();
    
                            //gestione searchHotel : cerchiamo l'hotel che viene riferito dalla recensione, se lo troviamo facciamo compilare i voti, altrimenti segnaliamo errore
    
                            List<Hotel> listaHotel = db.scanHotels();

                            ClientMessage hotelmess; 
                            while ((hotelmess = (ClientMessage) in.readObject()) == null){}

                            if (hotelmess instanceof ControlClientMessage){
                                ControlClientMessage cod = (ControlClientMessage) hotelmess; 
                                if (cod.getCod() == 9){
                                    //chiudi la sessione (cod. 9)
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
                            }

                            if (hotelmess instanceof StringClientMessage){
                                StringClientMessage hotelMessage = (StringClientMessage) hotelmess; 
                                String nomeHotel = hotelMessage.getMess(); 

                                if (nomeHotel.length() == 0) {
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
                                    out.writeObject(new ControlServerMessage(1)); //hotel trovato
                                    out.flush();
                                } else {
                                    out.writeObject(new ControlServerMessage(0)); // messaggio di errore nomeHotel
                                    out.flush();
                                    break; 
                                }

                                ClientMessage citymess = null; 
                                while ((citymess = (ClientMessage) in.readObject()) == null){}

                                if (citymess instanceof ControlClientMessage){
                                    ControlClientMessage cod = (ControlClientMessage) hotelmess; 
                                    if (cod.getCod() == 9){
                                        //chiudi la sessione (cod. 9)
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
                                }

                                if (citymess instanceof StringClientMessage){
                                    StringClientMessage cityMessage = (StringClientMessage) citymess; 
                                    String città = cityMessage.getMess(); 
                                    
                                    if (città.length() == 0) break;
                                    
                                    boolean f_città = false;
            
                                    for (Hotel h : listaHotel) {
                                        if (h.getName().equals(nomeHotel)) {
                                            if (h.getCity().equals(città)) {
                                                f_città = true;
                                                break;
                                            }
                                        }
                                    }

                                    if (f_città) {
                                        out.writeObject(new ControlServerMessage(1)); //città trovata
                                        out.flush();
                                    } else {
                                        out.writeObject(new ControlServerMessage(0)); // messaggio di errore città
                                        out.flush();
                                        break; 
                                    }  
                                    
                                    ClientMessage reviewmess = null; 
                                    while ((reviewmess = (ClientMessage) in.readObject()) == null){}

                                    if (reviewmess instanceof ControlClientMessage){
                                        ControlClientMessage cod = (ControlClientMessage) hotelmess; 
                                        if (cod.getCod() == 9){
                                        //chiudi la sessione (cod. 9)
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
                                    }

                                    if (reviewmess instanceof ReviewClientMessage){
                                        ReviewClientMessage reviewMessage = (ReviewClientMessage) reviewmess; 
                                        Recensione review = reviewMessage.getReview(); 
 
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
                                            out.writeObject(new ControlServerMessage(0)); //messaggio di errore : errore sui campi
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
                
                                        out.writeObject(new ControlServerMessage(1)); //operazione conclusa
                                        out.flush();
                                        break;
                                    }
                                }
                            }   
                        }
    
                        case 7: // showBadge
                        {
                            if ((user != null && !user.isLogged()) || user == null) {
                                out.writeObject(new ControlServerMessage(0));; // non si può visualizzare il distintivo
                                out.flush();
                                break;
                            }
    
                            // mandiamo il badge
                            out.writeObject(new ControlServerMessage(1));
                            out.writeObject(new StringServerMessage(user.getBadge()));
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



    /**
     * Funzione usata per mandare un messaggio di fine comunicazione al task delle notifiche del client.
     * 
     * @param m, rappresenta il socket multicast su cui inoltriamo il datagramma 
     */
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
