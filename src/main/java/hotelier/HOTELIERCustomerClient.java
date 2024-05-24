package hotelier;

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


public class HOTELIERCustomerClient {

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
            NotificheTask task = null;
           // NotificheTask sistemaNotifiche = null;

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

                out.writeInt(scelta); // mando la scelta che ha fatto il client alla sessione, che la gestisce a
                                      // seconda dello stato (autenticato o no)
                out.flush();

                switch (scelta) {
                    case 1: // registrazione
                    {
                        int res = -1;
                        while ((res = in.readInt()) == -1) {
                        } // attesa

                        if (res == 1) { // possiamo registrarsi
                            System.out.println("Scrivere le credenziali in un formato adeguato.");
                            System.out.print("Immetti username : ");
                            String user = b.readLine();
                            System.out.print("Immetti password : ");
                            String pass = b.readLine();
                            System.out.println();

                            register(new Utente(user, pass), in, out);

                        } else {
                            System.out.println("\n"+redColor+"Non puoi registrarti se sei autenticato! Fai logout"+resetColor+"\n");
                        }

                        break;

                    }

                    case 2: // login
                    {
                        int res = -1;
                        while ((res = in.readInt()) == -1) {
                        } // attesa

                        if (res == 1) { // possiamo loggarci
                            System.out.println("Scrivere le credenziali in un formato adeguato.");
                            System.out.print("Immetti username : ");
                            String user = b.readLine();
                            char[] passArray = System.console().readPassword("Inserisci la password: ");
                            String pass = new String(passArray);
                            System.out.println();
    
                                auth_user = login(new Utente(user, pass), in, out, task);
                            
                            if (auth_user != null){
                                System.out.println("\n"+greenColor+"Utente autenticato!"+resetColor+"\n");
                                cleanupTask = new CleanupTask(auth_user);
                                Runtime.getRuntime().addShutdownHook(cleanupTask);                     
                            }

                        } else {
                            System.out.println(
                                "\n"+redColor+"Sei già autenticato! Per autenticarsi con un altro account effettuare il logout"+resetColor+"\n");
                        }
                        break;
                    }

                    case 3: // logout
                    {
                        int res = -1;
                        while ((res = in.readInt()) == -1) {
                        } // attesa

                        if (res == 1){
                            auth_user = null; 
                            if (cleanupTask != null) Runtime.getRuntime().removeShutdownHook(cleanupTask); //tolgo la funzione di cleanup
                            if (task != null) {
                               task = null;   
                            } 

                            System.out.println("\n"+greenColor+"Logout effettuato con successo!"+resetColor+"\n");
                        }
                            
                        else
                            System.out.println("\n"+redColor+"Non puoi effettuare logout se non sei autenticato!"+resetColor+"\n");
                        break;
                    }

                    case 4: // searchHotel
                    {
                        // manda nome dell'hotel e città
                        System.out.print("Inserisci il nome dell'hotel: ");
                        String nomeHotel = b.readLine();
                        System.out.print("Inserisci la città: ");
                        String città = b.readLine();

                        Hotel daStampare = searchHotel(nomeHotel, (città.substring(0, 1).toUpperCase() + città.substring(1)).trim(), in, out);
                        if (daStampare != null) {
                            int numRecensioni = in.readInt(); 
                            System.out.println("\n"+greenColor+"Ho trovato le seguenti informazioni:"+resetColor+"\n");
                            visualizzaDettagliHotel(daStampare, numRecensioni);
                        } else {
                            System.out.println("\n"+redColor+"Hotel non trovato"+resetColor+"\n");
                        }
                        break;
                    }

                    case 5: // searchAllHotels
                    {
                        System.out.print("Inserisci la città: ");
                        String città = b.readLine();

                        searchAllHotels((città.substring(0, 1).toUpperCase() + città.substring(1)).trim(), in, out);
                        break;

                    }

                    case 6: // insertReview (Il client inserisce una recensione)
                    {
                        int res = -1;
                        while ((res = in.readInt()) == -1) {
                        } // attesa

                        if (res == 0){
                            System.out.println("\n"+redColor+"Non sei autenticato! Non puoi effettuare recensioni"+resetColor+"\n");
                            break; 
                        }

                        System.out.print("Inserisci il nome dell'hotel : ");
                        String nomeHotel = b.readLine();
                        System.out.print("Inserisci la città: ");
                        String città = b.readLine();

                        if (!checkHotel(nomeHotel, (città.substring(0,1).toUpperCase()+città.substring(1)).trim(), in, out)){
                            System.out.println("\n"+redColor+"Hotel non trovato"+resetColor+"\n");
                            break;
                        }

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
                        int res = -1;
                        while ((res = in.readInt()) == -1) {
                        } // attesa risposta della sessione

                        if (res == 1) {
                            String badge;
                            while ((badge = in.readUTF()).isEmpty()) {
                            }

                            System.out.println();
                            System.out.println("Hai il seguente distintivo : " +greenColor+ badge +resetColor+ " \n");
                        } else {
                            System.out.println();
                            System.out.println("\n"+redColor +"Non sei autenticato! Fai login"+ resetColor+"\n");
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
            out.writeObject(u); //credenziali inviate
            int res = -1;
            while ((res = in.readInt()) == -1) {
            } // attesa
            switch (res) {
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
                    String mess;
                    while ((mess =  in.readUTF()).isEmpty()){}

                    System.out.println("\n"+redColor+mess+resetColor+"\n");
                    break; 
                }
                       
                default:
                    break;
            }       
                     
        } catch (IOException e) {
            e.printStackTrace();
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
            out.writeObject(u);

            System.out.print("Credenziali inviate");
            for (int i=0; i<3; i++){
                try{
                    Thread.sleep(1000);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                System.out.print(". ");
            }

            int res = -1;
            while ((res = in.readInt()) == -1) {
            } // attesa
            
            switch (res){
                case 0: {
                    //messaggio di autenticazione non riuscita
                    System.out.println("\n"+redColor+"Autenticazione fallita!"+resetColor+"\n");
                    return null; 
                }
                case 1: {
                    //autenticazione riuscita
                    Utente auth_user = (Utente) in.readObject(); 
                    MulticastSocket m = new MulticastSocket(port);
                    task = new NotificheTask(m, group);
                    e.execute(task); //faccio partire il task che attende le notifiche e le stampa
            
                    return auth_user;   
                }
                case 2: {
                    //messaggio di errore sulle credenziali
                    String mess;
                    while ((mess =  in.readUTF()).isEmpty()){}

                    System.out.println("\n"+redColor+mess+resetColor+"\n");
                    return null;
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
            out.writeUTF(nomeHotel);
            out.flush();

            int r1 = -1;
            while ((r1 = in.readInt()) == -1) {
            }

            if (r1 == 1) {
                // hotel presente nel file
                out.writeUTF(città);
                out.flush();

                int r2 = -1;
                while ((r2 = in.readInt()) == -1) {
                }

                if (r2 == 1) {
                    // hotel trovato : abbiamo controllato se l'hotel appartiene alla città mandata in input
                    Object inObject;
                    while ((inObject = in.readObject()) == null) {
                    }

                    if (inObject instanceof Hotel)
                        res = (Hotel) inObject;
                }else return null; 
            }else return null; 
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
        }

        return res;
    }

    public static boolean checkHotel(String nomeHotel, String città, ObjectInputStream in, ObjectOutputStream out) {

        boolean res = false;
        try {
            out.writeUTF(nomeHotel);
            out.flush();

            int r1 = -1;
            while ((r1 = in.readInt()) == -1) {
            }

            if (r1 == 1) {
                // hotel presente nel file
                out.writeUTF(città);
                out.flush();

                int r2 = -1;
                while ((r2 = in.readInt()) == -1) {
                }

                if (r2 == 1) {
                    // hotel trovato
                    Object inObject;
                    while ((inObject = in.readObject()) == null) {
                    }

                    if (inObject instanceof Hotel)
                        res = true; 
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
        }

        return res;
    }

    public static void searchAllHotels(String città, ObjectInputStream in, ObjectOutputStream out) {
        try {
            out.writeUTF(città); // mando il nome al server (Sessione)
            out.flush();

            int res = -1;
            while ((res = in.readInt()) == -1) {
            } // wait

            if (res == 1) {

                System.out.println("\n"+greenColor+"Ho trovato i seguenti Hotel: "+resetColor+"\n");
                
                Hotel hotel;

                while ((hotel = (Hotel) in.readObject()) != null){ 
                    int numRecensioni = in.readInt();
                    visualizzaDettagliHotel(hotel, numRecensioni);  
                }

            } else {
                System.out.println("\n"+redColor+"Non sono stati trovati hotel per la determinata città"+resetColor+"\n");
                System.out.println();
            }        
                
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException ce) {
            ce.printStackTrace();
        }
    }

    /**
     * Funzione usata per memorizzare la Recensione dentro il file json (Recensioni.json).
     * 
     * @param reviewHotel  , Hotel per il quale sto facendo la recensione
     * @param globalScore  , score globale (complessivo)
     * @param singleScores , voti riguardanti le singole categorie (pulizia,
     *                     servizi, posizione e qualità)
     */
    public static void insertReview(String nomeHotel, String città, double globalScore, Ratings singleScores, ObjectInputStream in,
            ObjectOutputStream out) {
                
        // procediamo con l'inserimento della Recensione: incapsuliamo in un oggetto Recensione e mandiamola alla sessione, che farà gli opportuni controlli sulla struttura
        Recensione r = new Recensione(nomeHotel, città, singleScores, globalScore);
        try{
            out.writeObject(r);
            out.flush();

            int res = -1; 
            while ((res = in.readInt()) == -1){}

            if (res == 1) System.out.println("\n"+greenColor+"Recensione effettuata!"+resetColor+"\n");
            else System.out.println("\n"+redColor+"Errore ! Controlla di aver inserito bene i voti"+resetColor+"\n");

        }catch (IOException e){
            e.printStackTrace();
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
