package hotelier.Messages.ClientMessages;

import hotelier.Structures.Recensione;

public class ReviewClientMessage extends ClientMessage{
    Recensione review;

    public ReviewClientMessage(Recensione review) {
        this.review = review;
    }

    public Recensione getReview() {
        return review;
    }    
}
