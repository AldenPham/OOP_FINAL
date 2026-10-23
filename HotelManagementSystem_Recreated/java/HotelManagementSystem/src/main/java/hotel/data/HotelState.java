package hotel.data;

import hotel.model.*;
import java.util.ArrayList;
import java.util.List;

public final class HotelState {
    public final List<Customer> customers = new ArrayList<>();
    public final List<RoomGroup> roomGroups = new ArrayList<>();
    public final List<Utility> utilities = new ArrayList<>();
    public final List<Room> rooms = new ArrayList<>();
    public final List<RentalContract> contracts = new ArrayList<>();
    public final List<Invoice> invoices = new ArrayList<>();
}
