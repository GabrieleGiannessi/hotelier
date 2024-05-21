package hotelier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Questo task viene eseguito ogni qualvolta se esca dalla sessione in maniera "brutale": 
 * è una sorta di codice di cleanup che viene avviato per salvare le informazioni del client che si era autenticato al servizio
 */

public class CleanupTask extends Thread{

    Utente u; //utente di cui devo salvare i dati

    public CleanupTask(Utente u) { 
        this.u = u;
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
                    u.setLogged(false); //logout forzato
                    saveUtente(u);
                    System.out.println("Informazioni salvate!");
                    System.out.println();
                }  
    }

    public synchronized static List<Utente> scanUtenti() {
        String UtentiPath = "Utenti.json";
        List<Utente> utenti = new ArrayList<Utente>();
        try {
            Type utenteListType = new TypeToken<List<Utente>>() {
            }.getType();
            utenti = new Gson().fromJson(new FileReader(UtentiPath), utenteListType);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return utenti;
    }

    //salva i dati dell'utente (identificato tramite l'oggetto) nel file utenti.json
    public void saveUtente (Utente aggiornato){
        List<Utente> listaUtenti = scanUtenti(); 
        if (listaUtenti !=  null){
            for (int i = 0; i < listaUtenti.size(); i++){
                Utente u = listaUtenti.get(i);
                if (u.getUsername().equals(aggiornato.getUsername())){
                    synchronized(this){
                        listaUtenti.set(i, aggiornato);
                    }
                }
            }
            
            //risalvo la lista
        File inputUtenti = new File(
                "Utenti.json");
        try (FileWriter writer = new FileWriter(inputUtenti)) {
            new Gson().toJson(listaUtenti, writer);
        } catch (IOException e) {
            e.printStackTrace();
            }
        }  
    }


}
