package hotelier;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class NotificheTask implements Runnable {

    private MulticastSocket m;
    public String group;
    public volatile boolean run = true;
    public volatile boolean interrupted = false;

    public NotificheTask(MulticastSocket m, String group) {
        this.m = m;
        this.group = group;
    }

    @Override
    public void run() {
        try {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            // Ciclo per ricevere le notifiche
            while (run) {
                if (!interrupted)
                    m.receive(packet); // Questo metodo bloccherà l'esecuzione finché non arriverà un pacchetto dal
                                       // server

                // Elabora il pacchetto ricevuto
                String message = new String(packet.getData(), 0, packet.getLength());

                if (message.equals("[FINE]"))
                    break;
                System.out.println("[NOTIFICA] " + message);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    public void stopRun() {
        run = false;
        interrupted = true;
        try {
            m.leaveGroup(InetAddress.getByName(group)); // Lascia il gruppo multicast prima di chiudere il socket
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        m.close();
    }

}
