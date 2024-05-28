package hotelier.Messages.ClientMessages;

public class ControlClientMessage extends ClientMessage{
    int cod;  

    public ControlClientMessage (int cod){
        this.cod=cod; 
    }

    public int getCod() {
        return cod;
    }   
}
