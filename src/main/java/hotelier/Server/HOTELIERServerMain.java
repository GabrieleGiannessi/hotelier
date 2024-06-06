package hotelier.Server;


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

public class HOTELIERServerMain {

    private static final String configFile = "./src/main/resources/server.properties"; 
    private static int port; 
    private static String group;
    private static int numSecondi; 
    private static JsonDB db = JsonDB.getInstance(); //database

    private static ExecutorService threadPool = new ThreadPoolExecutor(4, 10, 1, TimeUnit.SECONDS,
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
        scheduler.scheduleAtFixedRate(new CalcoloRanking(m, group, port, db), 0, numSecondi, TimeUnit.SECONDS); //CalcoloRanking : task che si occupa di calcolare il ranking e inviare le notifiche
            while (true) { // rimane sempre attivo
                threadPool.execute(new Sessione(s.accept(), m, group, port, db)); // threads con cui stabilisco la connessione con i client
                System.out.println("Client connesso");
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
        numSecondi = Integer.parseInt(prop.getProperty("numSecondi"));
        in.close(); 
    }
}



