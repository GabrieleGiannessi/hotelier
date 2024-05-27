package hotelier;

import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Questo task viene eseguito ogni qualvolta se esca dalla sessione in maniera "brutale": 
 * è una sorta di codice di cleanup che viene avviato per salvare le informazioni del client che si era autenticato al servizio
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

                //mando alla sessione un codice di uscita speciale (in quanto bisogna salvare i dati dell'utente e settare il logout forzato)
                try{
                    out.writeInt(9); 
                    out.flush();
                }catch (IOException e){
                    System.err.println("Errore durante invio codice di cleanup alla sessione");
                    e.printStackTrace(); 
                }

                    System.out.println();
                    System.out.println("Informazioni salvate!");          
                }  
        }
}
