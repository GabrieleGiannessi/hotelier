package hotelier.Messages.ServerMessages;

import hotelier.Structures.Hotel;

public class HotelServerMessage extends ServerMessage{
    Hotel hotel;

    public HotelServerMessage(Hotel hotel) {
        this.hotel = hotel;
    }

    public Hotel getHotel() {
        return hotel;
    }   
}
