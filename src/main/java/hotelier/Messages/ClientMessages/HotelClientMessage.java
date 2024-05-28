package hotelier.Messages.ClientMessages;

import hotelier.Structures.Hotel;

public class HotelClientMessage extends ClientMessage{
    Hotel hotel;

    public HotelClientMessage(Hotel hotel) {
        this.hotel = hotel;
    }

    public Hotel getHotel() {
        return hotel;
    } 
    
}
