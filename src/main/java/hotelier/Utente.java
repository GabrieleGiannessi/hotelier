package hotelier;

import java.io.Serializable;

public class Utente implements Serializable{
    
    private String username; 
    private String password; 
    private int numeroRecensioni = 0; 
    private String badge = "Recensore"; 
    private boolean isLogged = false;

    public Utente (String n, String p){
        this.username = n; 
        this.password = p; 
    }
 
    public String getUsername() {
        return username;
    }

    public void setNome(String nome) {
        this.username= nome;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getNumeroRecensioni() {
        return numeroRecensioni;
    }

    public void setNumeroRecensioni(int numeroRecensioni) {
        this.numeroRecensioni = numeroRecensioni;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public boolean isLogged() {
        return isLogged;
    }

    public void setLogged(boolean isLogged) {
        this.isLogged = isLogged;
    } 
}
