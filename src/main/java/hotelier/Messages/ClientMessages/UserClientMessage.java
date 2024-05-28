package hotelier.Messages.ClientMessages;

import hotelier.Structures.Utente;

public class UserClientMessage extends ClientMessage{
    Utente user;

    public UserClientMessage(Utente user) {
        this.user = user;
    }

    public Utente getUser() {
        return user;
    }    
}
