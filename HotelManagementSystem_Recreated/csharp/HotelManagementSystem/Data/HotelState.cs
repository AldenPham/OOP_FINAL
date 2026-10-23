using HotelManagementSystem.Models;

namespace HotelManagementSystem.Data;

public sealed class HotelState
{
    public List<Customer> Customers { get; } = [];
    public List<RoomGroup> RoomGroups { get; } = [];
    public List<Utility> Utilities { get; } = [];
    public List<Room> Rooms { get; } = [];
    public List<RentalContract> Contracts { get; } = [];
    public List<Invoice> Invoices { get; } = [];
}
