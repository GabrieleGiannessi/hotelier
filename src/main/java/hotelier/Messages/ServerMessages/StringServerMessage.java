package hotelier.Messages.ServerMessages;

public class StringServerMessage extends ServerMessage{
    String message;

    public StringServerMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    } 
}
