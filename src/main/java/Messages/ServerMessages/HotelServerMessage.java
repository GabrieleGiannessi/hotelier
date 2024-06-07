package Messages.ServerMessages;

import Structures.Hotel;

public class HotelServerMessage extends ServerMessage{
    Hotel hotel;

    public HotelServerMessage(Hotel hotel) {
        this.hotel = hotel;
    }

    public Hotel getHotel() {
        return hotel;
    }   
}
