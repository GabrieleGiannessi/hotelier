package hotelier.Messages.ClientMessages;

public class StringClientMessage extends ClientMessage{
    String mess;

    public StringClientMessage(String mess) {
        this.mess = mess;
    }

    public String getMess() {
        return mess;
    }    
}
