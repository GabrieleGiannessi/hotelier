package hotelier.Client;

import java.io.IOException;
import java.io.ObjectOutputStream;

import hotelier.Messages.ClientMessages.ControlClientMessage;
import hotelier.Structures.Utente;

/**
 * Questo task viene eseguito ogni qualvolta se esca dalla sessione e l'utente rimane autenticato (causato da segnale di interruzione).
 * è una sorta di codice di cleanup che viene avviato per salvare le informazioni del client che si era autenticato al servizio.
 * Il funzionamento è il seguente: questo task viene registrato come shutdown hook nel thread principale del client, quando 
 * il client riceve un segnale di interruzione avvia questo codice, che provvede a mandare al server un messaggio di controllo.
 * Questo messaggio codifica un'uscita speciale in cui salviamo i dati dell'utente senza mandare messaggi UDP al task delle notifiche,
 * in quanto esso è già terminato attraverso il segnale di interruzione precedente. 
 */

public class CleanupTask extends Thread{

    Utente u; //utente di cui devo salvare i dati
    ObjectOutputStream out; 

    public CleanupTask(Utente u, ObjectOutputStream out) { 
        this.u = u;
        this.out = out; 
    }

    @Override
    public synchronized void run(){

        if (u != null && u.isLogged()){
            
            System.out.println();
            System.out.print("Salvataggio delle informazioni di "+ u.getUsername() + " ");
            for (int i= 0; i< 3; i++){
                try{
                    Thread.sleep(1000);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                System.out.print(". ");

            }
            System.out.println();
            System.out.println("Informazioni salvate!");          
        }  

                //mando alla sessione un codice di uscita speciale (in quanto bisogna salvare i dati dell'utente e settare il logout forzato)
                try{
                    out.writeObject(new ControlClientMessage(9)); 
                    out.flush();
                }catch (IOException e){
                    System.err.println("Errore durante invio codice di cleanup alla sessione");
                    e.printStackTrace(); 
                }
        }
}
