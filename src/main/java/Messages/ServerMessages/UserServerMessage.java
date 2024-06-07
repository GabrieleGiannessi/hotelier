package Messages.ServerMessages;

import Structures.Utente;

public class UserServerMessage extends ServerMessage{
    Utente user;

    public UserServerMessage(Utente user) {
        this.user = user;
    }

    public Utente getUser() {
        return user;
    }    
}
