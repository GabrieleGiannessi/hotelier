package Messages.ServerMessages;

public class ControlServerMessage extends ServerMessage{
    int cod; 

    public ControlServerMessage (int cod){
        this.cod = cod; 
    }

    public int getCod() {
        return cod;
    }
    
}
