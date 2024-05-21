package hotelier;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HOTELIERServer {

    /**
     * quando il server ricalcola i ranking locali, verifica se c’è un cambiamento
     * nella prima
     * posizione e, in questo caso, invia il nome del nuovo albergo che occupa la
     * prima posizione e
     * della città in cui si trova, a tutti gli utenti loggati. La trasmissione
     * avviene tramite l’invio di un
     * pacchetto UDP su un gruppo di multicast a cui sono iscritti tutti gli utenti
     * loggati.
     * 
     * ● il server può essere realizzato con JAVA I/O e threadpool oppure può
     * effettuare il
     * multiplexing dei canali mediante NIO (eventualmente con threadpool per la
     * gestione delle
     * richieste).
     * 
     * ● Il server definisce opportune strutture dati per memorizzare le
     * informazioni relative agli
     * utenti registrati, agli hotel, alle recensioni. Tali informazioni devono
     * essere persistite
     * periodicamente.
     * 
     * ● i file per la memorizzazione degli utenti e dei contenuti memorizzano le
     * informazioni in
     * formato JSON.
     */
    // file che uso per creare le strutture dati

    private static final String configFile = "server.properties"; 
    private static int port; 
    private static String group;

    private static ExecutorService threadPool = new ThreadPoolExecutor(4, 4, 1, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());
    private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(); //questo pool viene usato per eseguire il thread che calcola il ranking 

    public static void main(String[] args) {
        try{
            readConfig(); 
        }catch (FileNotFoundException f){
            System.err.println("Errore durante la lettura del file di configurazione");
            System.exit(0);
        }catch (IOException e){
            System.err.println("Errore generico input/output");
            System.exit(0);
        }
    

        System.out.println("Server HOTELIER attivo\n");

        try (ServerSocket s = new ServerSocket(port); 
        MulticastSocket m = new MulticastSocket()) {
        scheduler.scheduleAtFixedRate(new CalcoloRanking(m, group, port), 0, Integer.parseInt(args[0]), TimeUnit.SECONDS); //CalcoloRanking : task che si occupa di calcolare il ranking e inviare le notifiche
            while (true) { // rimane sempre attivo
                threadPool.execute(new Sessione(s.accept(), m, group, port)); // threads con cui stabilisco la connessione con i client
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
            scheduler.shutdown();
        }
    }

    private static void readConfig() throws FileNotFoundException, IOException {
        InputStream in = new FileInputStream(configFile); 
        Properties prop = new Properties(); 
        prop.load(in); 
        port = Integer.parseInt(prop.getProperty("port"));
        group = prop.getProperty("group"); 
        in.close(); 
    }
}



