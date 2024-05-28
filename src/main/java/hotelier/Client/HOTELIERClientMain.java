package hotelier.Client;

import hotelier.Structures.*; 
import hotelier.Messages.ClientMessages.*;
import hotelier.Messages.ServerMessages.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MulticastSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class HOTELIERClientMain{

    private static final String configFile = "client.properties"; 
    private static int port; 
    private static String ipServer;
    private static String group;
    private static final ExecutorService e = Executors.newFixedThreadPool(1); //pool notifiche
    private static String redColor;
    private static String greenColor;
    private static String lightBlueColor;
    private static String resetColor;
    //private static NotificheTask task = null; 

    public static void main(String[] args) {

        try{
            readConfig(); 
        }catch (IOException e){
            System.err.println("Errore durante la lettura del file di configurazione");
            System.exit(0);
        }         

        Utente auth_user = null; //informazioni riguardanti l'utente autenticato (Client)
        try (Socket s = new Socket(ipServer, port);
                ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                BufferedReader b = new BufferedReader(new InputStreamReader(System.in))) {

            int scelta = 0;
            boolean exit = false;
            Thread cleanupTask = null; //thread usato per la funzione di cleanup
            NotificheTask task = null; //thread notifiche

            System.out.println();
            System.out.println();
            System.out.println();

                System.out.println("    /       /    -------      /  *          ---");
                System.out.println("   /       / ----  /   /---  /  /    /---  /  /");
                System.out.println("  /-------/ /   / /   /     /  /    /     /  /");
                System.out.println(" /       / /   / /   /---  /  /    /---  /-----");
                System.out.println("/       / ----  /   /---  ---     /---  /     /");

            System.out.println();
            System.out.println();
            System.out.println();

            do {

                System.out.println("\nQuale operazione vuoi eseguire?");
                System.out.println("1) Registrazione ");
                System.out.println("2) Login ");
                System.out.println("3) Logout ");
                System.out.println("4) Visualizza un determinato hotel di una città ");
                System.out.println("5) Visualizza gli hotel di una città ");
                System.out.println("6) Inserisci una recensione ");
                System.out.println("7) Visualizza distintivo ");
                System.out.println("8) Esci ");

                System.out.print("\nScelta: ");
                scelta = Integer.parseInt(b.readLine());
                System.out.println();

                out.writeObject(new ControlClientMessage(scelta)); // incapsulo la scelta  che ha fatto il client in un messaggio di controllo e lo mando alla sessione, che la gestisce a
                                      // seconda dello stato (autenticato o no)
                out.flush();

                switch (scelta) {
                    case 1: // registrazione
                    {
                        ServerMessage res = null;
                        while ((res = (ServerMessage) in.readObject()) == null) {
                        } // attesa

                        if (res instanceof ControlServerMessage) { 
                            ControlServerMessage serverMessage = (ControlServerMessage) res; 
                            if ( serverMessage.getCod() == 1){ // possiamo registrarsi
                                System.out.println("Scrivere le credenziali in un formato adeguato.");
                                System.out.print("Immetti username : ");
                                String user = b.readLine();
                                System.out.print("Immetti password : ");
                                String pass = b.readLine();
                                System.out.println();
                                register(new Utente(user, pass), in, out);
                            }else {
                                System.out.println("\n"+redColor+"Non puoi registrarti se sei autenticato! Fai logout"+resetColor+"\n");
                            }
                        } 

                        break;

                    }

                    case 2: // login
                    {
                        ServerMessage res = null;
                        while ((res = (ServerMessage) in.readObject()) == null) {
                        } // attesa

                        if (res instanceof ControlServerMessage){
                            ControlServerMessage serverMessage = (ControlServerMessage) res; 
                            if (serverMessage.getCod() == 1){
                                System.out.println("Scrivere le credenziali in un formato adeguato.");
                                System.out.print("Immetti username : ");
                                String user = b.readLine();
                                char[] passArray = System.console().readPassword("Inserisci la password: ");
                                String pass = new String(passArray);
                                System.out.println();
    
                                auth_user = login(new Utente(user, pass), in, out, task);
                            
                            if (auth_user != null){

                                System.out.println("\n"+greenColor+"Utente autenticato!"+resetColor+"\n");
                                cleanupTask = new CleanupTask(auth_user, out);
                                Runtime.getRuntime().addShutdownHook(cleanupTask);                     
                            }

                        } else {
                            System.out.println(
                                "\n"+redColor+"Sei già autenticato! Per autenticarsi con un altro account effettuare il logout"+resetColor+"\n");
                            }
                        }     
                        break;
                    }

                    case 3: // logout
                    {
                        ServerMessage res = null;
                        while ((res = (ServerMessage) in.readObject()) == null) {
                        } // attesa

                        if (res instanceof ControlServerMessage){
                            ControlServerMessage serverMessage = (ControlServerMessage) res; 
                            if (serverMessage.getCod() == 1){
                                auth_user = null; 
                                if (cleanupTask != null) Runtime.getRuntime().removeShutdownHook(cleanupTask); //tolgo la funzione di cleanup

                                if (task != null) {
                                    task = null;   
                                }
                                System.out.println("\n"+greenColor+"Logout effettuato con successo!"+resetColor+"\n");
                            }else System.out.println("\n"+redColor+"Non puoi effettuare logout se non sei autenticato!"+resetColor+"\n");
                        }
                        break;
                        }
                        
                    case 4: // searchHotel
                    {
                        // manda nome dell'hotel e città
                        System.out.print("Inserisci il nome dell'hotel: ");
                        String nomeHotel = b.readLine();
                        System.out.print("Inserisci la città: ");
                        String città = b.readLine();

                        if (nomeHotel.length() == 0 && città.length() == 0){
                            System.out.println("\n"+redColor+"Nome hotel e città non inseriti!"+resetColor+"\n");
                            out.writeObject(new StringClientMessage(città)); 
                            out.flush();
                            break;
                        }

                        if (nomeHotel.length() == 0){
                            System.out.println("\n"+redColor+"Hotel non inserito!"+resetColor+"\n");
                            out.writeObject(new StringClientMessage(città)); 
                            out.flush();
                            break;
                        }

                        if (città.length() == 0) {
                            System.out.println("\n"+redColor+"Città non inserita!"+resetColor+"\n");
                            out.writeObject(new StringClientMessage(città)); 
                            out.flush();
                            break; 
                        }

                        Hotel daStampare = searchHotel(nomeHotel, (città.substring(0, 1).toUpperCase() + città.substring(1)).trim(), in, out);

                        if (daStampare != null) {
                            int numRecensioni = in.readInt(); 
                            System.out.println("\n"+greenColor+"Ho trovato le seguenti informazioni:"+resetColor+"\n");
                            visualizzaDettagliHotel(daStampare, numRecensioni);
                        } 

                        break;
                    }

                    case 5: // searchAllHotels
                    {
                        System.out.print("Inserisci la città: ");
                        String città = b.readLine();

                        if (città.length() == 0) {
                            System.out.println("\n"+redColor+"Città non inserita!"+resetColor+"\n");
                            out.writeObject(new StringClientMessage(città)); 
                            out.flush();
                            break;
                        }

                        searchAllHotels((città.substring(0, 1).toUpperCase() + città.substring(1)).trim(), in, out);
                        break;

                    }

                    case 6: // insertReview (Il client inserisce una recensione)
                    {
                        ServerMessage res = null;
                        while ((res = (ServerMessage) in.readObject()) == null) {
                        } // attesa

                        if (res instanceof ControlServerMessage){
                            ControlServerMessage controlMessage = (ControlServerMessage) res; 

                            if (controlMessage.getCod() == 0){
                                System.out.println("\n"+redColor+"Non sei autenticato! Non puoi effettuare recensioni"+resetColor+"\n");
                                break; 
                            }
                        }

                        System.out.print("Inserisci il nome dell'hotel : ");
                        String nomeHotel = b.readLine();
                        System.out.print("Inserisci la città: ");
                        String città = b.readLine();

                        if (città.length() == 0 && nomeHotel.length() == 0) {
                            System.out.println("\n"+redColor+"Campi non inseriti!"+resetColor+"\n");
                            out.writeObject(new StringClientMessage(città));  
                            out.flush();
                            break; 
                        }

                        if (nomeHotel.length() == 0){
                            System.out.println("\n"+redColor+"Hotel non inserito!"+resetColor+"\n");
                            out.writeObject(new StringClientMessage(città));  
                            out.flush();
                            break;
                        }

                        if (città.length() == 0) {
                            System.out.println("\n"+redColor+"Città non inserita!"+resetColor+"\n");
                            out.writeObject(new StringClientMessage(città)); 
                            out.flush();
                            break; 
                        }

                        if (!checkHotel(nomeHotel, (città.substring(0,1).toUpperCase()+città.substring(1)).trim(), in, out)) break;
                        
                        System.out.println("Inserisci i voti riguardanti i vari servizi");
                        System.out.print("Pulizia: ");
                        double cleaning = Double.parseDouble(b.readLine());
                        System.out.print("Posizione: ");
                        double position = Double.parseDouble(b.readLine());
                        System.out.print("Servizi: ");
                        double services = Double.parseDouble(b.readLine());
                        System.out.print("Qualità: ");
                        double quality = Double.parseDouble(b.readLine());
                        System.out.print("Inserisci il voto finale: ");
                        double globalScore = Double.parseDouble(b.readLine());
                        System.out.println();

                        insertReview(nomeHotel, (città.substring(0,1).toUpperCase()+città.substring(1)).trim(), globalScore, new Ratings(cleaning, position, services, quality), in,
                                out);
                        break;
                    }

                    case 7: // showBadge
                    {
                        ServerMessage res = null;
                        while ((res = (ServerMessage) in.readObject()) == null) {
                        } // attesa risposta della sessione

                        if (res instanceof ControlServerMessage){
                            ControlServerMessage serverMessage = (ControlServerMessage) res; 
                            if (serverMessage.getCod() == 1){
                                StringServerMessage badge;
                                while ((badge = (StringServerMessage) in.readObject()) == null) {
                                }

                                System.out.println();
                                System.out.println("Hai il seguente distintivo : " +greenColor+ badge.getMessage() +resetColor+ " \n");
                            } else {
                                System.out.println();
                                System.out.println("\n"+redColor +"Non sei autenticato! Non puoi visualizzare il distintivo"+ resetColor+"\n");
                                }
                            }
                            break;
                        }

                    case 8: // Exit : comunico al server di chiudere la sessione
                    { 
                        exit = true;
                        auth_user = null; 
                        if (cleanupTask != null) Runtime.getRuntime().removeShutdownHook(cleanupTask); //tolgo la funzione di cleanup  
                        if (task != null) task = null;
                            
                        break;
                    }

                    default:
                        System.out.println("\n"+redColor +"Scelta non valida, riprova"+ resetColor+"\n");
                        break;
                }

            } while (!exit);

        } catch (IOException e) {
            System.err.println("\n"+redColor + "Errore in ingresso o in uscita dal client" + resetColor+"\n");
            System.exit(0);
        } catch (NumberFormatException n){
            System.err.println("\n"+redColor + "Errore, la scelta viene effettuata inserendo un numero!" + resetColor+"\n");
            System.exit(0);
        } catch (ClassNotFoundException c){
            System.err.println("\n"+redColor + "Errore, classe non trovata in input!" + resetColor+"\n");
            System.exit(0);
        }

        //termino il threadpool
        e.shutdown();
        try{
             e.awaitTermination(5,TimeUnit.SECONDS);
         }catch (InterruptedException e){
             e.printStackTrace();
        }   
    }

    private static void readConfig() throws FileNotFoundException, IOException {
        InputStream in = new FileInputStream(configFile); 
        Properties prop = new Properties(); 
        prop.load(in); 
        port = Integer.parseInt(prop.getProperty("port"));
        ipServer = prop.getProperty("ipServer"); 
        group = prop.getProperty("group"); 
        redColor = prop.getProperty("redColor");
        greenColor = prop.getProperty("greenColor");
        lightBlueColor = prop.getProperty("lightBlueColor");
        resetColor = prop.getProperty("resetColor");
        
        in.close(); 
    }

    public static void register(Utente u, ObjectInputStream in, ObjectOutputStream out) {

        // mandiamo la stringa json sul server
        try {
            out.writeObject(new UserClientMessage(u)); //credenziali inviate
            ServerMessage res = null;
            while ((res = (ServerMessage) in.readObject()) == null) {
            } // attesa

            if (res instanceof ControlServerMessage){
                ControlServerMessage serverMessage = (ControlServerMessage) res; 
                switch (serverMessage.getCod()) {
                case 0:{
                    //utente già registrato
                    System.out.println("\n"+redColor +"Username già presente nel servizio"+resetColor+"\n");
                    break;
                }
                case 1:{
                    //messaggio di avvenuta registrazione
                    System.out.println("\n"+greenColor+"Operazione effettuata con successo"+resetColor+"\n");
                    break; 
                }

                case 2:{
                    //messaggio di errore sulle credenziali
                    StringServerMessage mess;
                    while ((mess = (StringServerMessage) in.readObject()) == null){}

                    System.out.println("\n"+redColor+mess.getMessage()+resetColor+"\n");
                    break; 
                }
                       
                default:
                    break;
            } 
            }
                  
                     
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException c){
            c.printStackTrace(); 
        }
    }

    /**
     * Funzione utilizzata per autenticare il client al servizio. 
     * Deve essere effettuata come prima operazione, dopo che è stata effettuata
     * la registrazione, usando una connessione TCP instaurata con il server. Il
     * login è necessario nel
     * caso si voglia pubblicare una recensione o vedere il proprio distintivo.
     * Dopo la login effettuata con successo, l’utente interagisce, secondo il
     * modello client-server
     * (richieste/risposte), con il server sulla connessione TCP persistente creata,
     * inviando uno dei comandi elencati nella sezione 2.1. Tutte le operazioni sono
     * richieste su
     * questa connessione TCP
     * 
     * @param u , Credenziali che mettiamo in input che riferiscono l'Utente
     * @param in
     * @param out , stream che usiamo per comunicare con il server (instaura una sessione col client)
     */

    public static Utente login(Utente u, ObjectInputStream in, ObjectOutputStream out, NotificheTask task) {
        try {

            out.writeObject(new UserClientMessage(u));

            System.out.print("Credenziali inviate");
            for (int i=0; i<3; i++){
                try{
                    Thread.sleep(1000);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                System.out.print(". ");
            }

            ServerMessage res = null;
            while ((res = (ServerMessage) in.readObject()) == null) {
            } // attesa
            
            if (res instanceof ControlServerMessage){
                ControlServerMessage serverMessage = (ControlServerMessage) res; 
                switch (serverMessage.getCod()){
                    case 0: {
                        //messaggio di autenticazione non riuscita
                        System.out.println("\n"+redColor+"Autenticazione fallita!"+resetColor+"\n");
                        return null; 
                    }

                    case 1: {
                        UserServerMessage userMessage = (UserServerMessage) in.readObject(); 
                        MulticastSocket m = new MulticastSocket(port);
                        task = new NotificheTask(m, group);
                        e.execute(task); //faccio partire il task che attende le notifiche e le stampa
                        return userMessage.getUser();   
                    }

                    case 2: {
                        //messaggio di errore sulle credenziali
                        StringServerMessage mess;
                        while ((mess = (StringServerMessage) in.readObject()) == null){}
    
                        System.out.println("\n"+redColor+mess.getMessage()+resetColor+"\n");
                        return null;
                    }
                }
            }      
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (ClassNotFoundException c){
            c.printStackTrace(); 
        }

        return null;
    }

    public static Hotel searchHotel(String nomeHotel, String città, ObjectInputStream in, ObjectOutputStream out) {

        Hotel res = null;
        try {
            out.writeObject(new StringClientMessage(nomeHotel));
            out.flush();

            ServerMessage r1 = null; //risposta alla stringa hotel
            while ((r1 = (ServerMessage) in.readObject()) == null) {}

            if (r1 instanceof ControlServerMessage){
                ControlServerMessage serverMessage = (ControlServerMessage) r1; 

                switch (serverMessage.getCod()) {
                    case 0:{
                        //hotel non trovato
                        System.out.println("\n"+redColor+"Hotel non trovato!"+resetColor+"\n");
                        return null;
                    }
                    case 1:{
                        // hotel presente nel file, ora si controlla la città
                        out.writeObject(new StringClientMessage(città));
                        out.flush();
    
                        ServerMessage r2 = null;
                        while ((r2 = (ServerMessage) in.readObject()) == null) {}
    
                        if (r2 instanceof ControlServerMessage){
                            ControlServerMessage serverMessage2 = (ControlServerMessage) r2; 
                            
                            switch (serverMessage2.getCod()){
                                case 0:{
                                    System.out.println("\n"+redColor+"Città non trovata!"+resetColor+"\n");
                                    return null;
                                }

                                case 1: {
                                    // hotel trovato : abbiamo controllato se l'hotel appartiene alla città mandata in input
                                HotelServerMessage hotelMessage; 
                                while ((hotelMessage = (HotelServerMessage) in.readObject()) == null) {
                                }
        
                                res = (Hotel) hotelMessage.getHotel();
                                break; 

                                }
                                
                                default:break;
                            }
                        }
                    }
                        
                    default:
                        break;
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
        }

        return res;
    }

    public static boolean checkHotel(String nomeHotel, String città, ObjectInputStream in, ObjectOutputStream out) {

        try {
            out.writeObject(new StringClientMessage(nomeHotel));
            out.flush();

            ServerMessage r1 = null;
            while ((r1 = (ServerMessage) in.readObject()) == null) {
            }

            if (r1 instanceof ControlServerMessage){
                ControlServerMessage hotelCod = (ControlServerMessage) r1; 

                switch (hotelCod.getCod()) {
                    case 0:{
                        //hotel non trovato
                        System.out.println("\n"+redColor+"Hotel non trovato!"+resetColor+"\n");
                        return false;
                    }

                    case 1:{
                        // hotel presente nel file, ora si controlla la città
                        out.writeObject(new StringClientMessage(città));
                        out.flush();
    
                        ServerMessage r2 = null;
                        while ((r2 = (ServerMessage) in.readObject()) == null) {
                        }

                        if (r2 instanceof ControlServerMessage){
                            ControlServerMessage citycod = (ControlServerMessage) r2; 

                            switch (citycod.getCod()){
                                case 0:{
                                    System.out.println("\n"+redColor+"Hotel non trovato nella seguente città!"+resetColor+"\n");
                                    return false;
                                }
                                case 1: {
                                    // hotel trovato : abbiamo controllato se l'hotel appartiene alla città mandata in input
                                    return true;     
                            }
                            default:break;
                        }
    
                    }
                }
    
                    default:
                        break;
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
        }

        return false;
    }

    public static void searchAllHotels(String città, ObjectInputStream in, ObjectOutputStream out) {
        try {
            out.writeObject(new StringClientMessage(città)); // mando il nome al server (Sessione)
            out.flush();

            ServerMessage res = null;

            while ( (res = (ServerMessage) in.readObject()) == null){}

            if (res instanceof ControlServerMessage){
                ControlServerMessage cod = (ControlServerMessage) res; 

                switch (cod.getCod()){
                    case 0: {
                        System.out.println("\n"+redColor+"Non sono stati trovati hotel per la determinata città"+resetColor+"\n");
                        System.out.println();
                        break; 
                    }
                    case 1: {
                        System.out.println("\n"+greenColor+"Ho trovato i seguenti Hotel: "+resetColor+"\n");
                    
                        HotelServerMessage hotelMessage;
        
                        while ((hotelMessage = (HotelServerMessage) in.readObject()) != null){ 
                            int numRecensioni = in.readInt();
                            visualizzaDettagliHotel(hotelMessage.getHotel(), numRecensioni);  
                        }
                        break; 
                    }
                    case 2: {
                        //errore di sintassi nei campi
                        StringServerMessage mess;
                        while ((mess =  (StringServerMessage) in.readObject()) == null){}

                        System.out.println("\n"+redColor+mess.getMessage()+resetColor+"\n");
                        break; 
                    }
                }                      
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException ce) {
            ce.printStackTrace();
        }
    }

    
    /**
     * Funzione attraverso il quale andiamo ad inserire la recensione nel sistema, andando ad inviare alla sessione la recensione, la quale farà gli opportuni controlli inviandoci un messaggio di ritorno
     * e opportunamente inserendo la recensione all'interno del db.
     * @param nomeHotel
     * @param città
     * @param globalScore
     * @param singleScores
     * @param in
     * @param out
     */
    public static void insertReview(String nomeHotel, String città, double globalScore, Ratings singleScores, ObjectInputStream in,
            ObjectOutputStream out) {
                
        // procediamo con l'inserimento della Recensione: incapsuliamo in un oggetto Recensione e mandiamola alla sessione, che farà gli opportuni controlli sulla struttura
        try{
            out.writeObject(new ReviewClientMessage(new Recensione(nomeHotel, città, singleScores, globalScore)));
            out.flush();

            ServerMessage res = (ServerMessage) in.readObject(); 

            if (res instanceof ControlServerMessage){
                ControlServerMessage cod = (ControlServerMessage) res; 

                switch (cod.getCod()){
                    case 0:{
                        System.out.println("\n"+redColor+"Errore ! Controlla di aver inserito bene i voti"+resetColor+"\n");
                        break;
                    }
                    case 1:{
                        System.out.println("\n"+greenColor+"Recensione effettuata!"+resetColor+"\n");
                        break;
                    }
                    default : break; 
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }catch (ClassNotFoundException c){
            c.printStackTrace();
        }   
    }

    public static void visualizzaDettagliHotel (Hotel h, int numRecensioni){

        Ratings ratingsHotel = h.getRatings(); 

         System.out.println();
         System.out.println(lightBlueColor+"* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"+resetColor);
         System.out.println("");
         System.out.println(lightBlueColor+"  Nome : "+resetColor+h.getName() +"");
         System.out.println(lightBlueColor+"  Città : "+resetColor+h.getCity() +"");
         System.out.println(lightBlueColor+"  Descrizione : "+resetColor+h.getDescription() +"");
         System.out.println(lightBlueColor+"  Numero di telefono : "+resetColor+h.getPhone() +"");
         if (!h.getServices().isEmpty()){
            System.out.println(lightBlueColor+"  Lista dei servizi : "+resetColor+"");
         for (String service : h.getServices()){
             System.out.println("\t♦ "+service+"");
        }
        }
         
         System.out.println();  
         System.out.println("  VOTI HOTEL ("+numRecensioni+" recensioni)");
         System.out.println(lightBlueColor+"  Pulizia : "+resetColor+ratingsHotel.getCleaning() +"");
         System.out.println(lightBlueColor+"  Posizione : "+resetColor+ratingsHotel.getPosition() +"");
         System.out.println(lightBlueColor+"  Qualità : "+resetColor+ratingsHotel.getQuality() +"");
         System.out.println(lightBlueColor+"  Servizi : "+resetColor+ratingsHotel.getServices() +"");
         System.out.println(lightBlueColor+"  Voto complessivo : "+resetColor+h.getRate() +"" );
         System.out.println();
         System.out.println(lightBlueColor+"* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"+resetColor);
         System.out.println();
    }

    
}
