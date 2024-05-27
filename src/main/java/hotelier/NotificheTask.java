package hotelier;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Properties;

public class NotificheTask implements Runnable {

    private static final String configFile = "client.properties"; 
    private static String resetColor; 
    private static String notifyColor; 
    private MulticastSocket m;
    public String group;
    public volatile boolean run = true;

    public NotificheTask(MulticastSocket m, String group) {
        this.m = m;
        this.group = group;
        try{
            readConfig(); 
        }catch (IOException e){
            System.err.println("Errore durante la lettura del file di configurazione");
            System.exit(0);
        }    
    }

    @SuppressWarnings("deprecation")
    @Override
    public void run() {
        try {
            m.joinGroup(InetAddress.getByName(group)); //mi iscrivo al gruppo multicast per ricevere le notifiche dal server
            
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            // Ciclo per ricevere le notifiche
            while (run) {
                    m.receive(packet); // Questo metodo bloccherà l'esecuzione finché non arriverà un pacchetto dal
                                       // server

                // Elabora il pacchetto ricevuto
                String message = new String(packet.getData(), 0, packet.getLength());

                if (message.equals("[FINE]")){
                    m.leaveGroup(InetAddress.getByName(group)); // Lascia il gruppo multicast prima di chiudere il socket
                    m.close();
                    break;
                }
                    
                System.out.println("\n\n"+notifyColor+"[NOTIFICA] " + message+resetColor+"\n");

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void readConfig() throws FileNotFoundException, IOException {
        InputStream in = new FileInputStream(configFile); 
        Properties prop = new Properties(); 
        prop.load(in); 
        resetColor = prop.getProperty("resetColor"); 
        notifyColor =  prop.getProperty("notifyColor"); 
        in.close(); 
    }
}
