using HotelManagementSystem.Models.Enums;

namespace HotelManagementSystem.Models;

public sealed class RentalContract(string contractId, Customer customer, Room room, DateTime startTime, DateTime endTime)
{
    private readonly List<Utility> _utilitiesUsed = [];
    public string ContractId { get; } = string.IsNullOrWhiteSpace(contractId) ? throw new ArgumentException("Contract ID cannot be blank.", nameof(contractId)) : contractId.Trim();
    public Customer Customer { get; } = customer ?? throw new ArgumentNullException(nameof(customer));
    public Room Room { get; } = room ?? throw new ArgumentNullException(nameof(room));
    public DateTime StartTime { get; } = startTime;
    public DateTime EndTime { get; } = endTime;
    public DateTime? CheckInTime { get; private set; }
    public DateTime? CheckOutTime { get; private set; }
    public ContractStatus Status { get; private set; } = ContractStatus.onGoing;
    public int RoomCapacityUsed { get; private set; }
    public IReadOnlyList<Utility> UtilitiesUsed => _utilitiesUsed.AsReadOnly();
    public void SetCheckInTime(DateTime value) => CheckInTime = value;
    public void SetCheckOutTime(DateTime value) => CheckOutTime = value;
    public void SetRoomCapacityUsed(int value) => RoomCapacityUsed = value;
    public void EndContract() => Status = ContractStatus.completed;
    public void SetStatus(ContractStatus status) => Status = status;
    public void AddUtility(Utility utility) { ArgumentNullException.ThrowIfNull(utility); _utilitiesUsed.Add(utility); }
    public override string ToString() => $"{ContractId} | customer: {Customer.Name} | room: {Room.RoomId} | {StartTime:yyyy-MM-dd HH:mm} -> {EndTime:yyyy-MM-dd HH:mm} | status: {Status}";
}
