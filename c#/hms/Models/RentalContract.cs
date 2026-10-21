using OOP_FINAL_CSharp.Models.Enums;

namespace OOP_FINAL_CSharp.Models;

public sealed class RentalContract
{
    private readonly List<Utility> _utilitiesUsed = [];

    public RentalContract(
        string contractId,
        Customer customer,
        Room room,
        DateTime startTime,
        DateTime endTime)
    {
        ContractId = contractId;
        Customer = customer ?? throw new ArgumentNullException(nameof(customer));
        Room = room ?? throw new ArgumentNullException(nameof(room));
        StartTime = startTime;
        EndTime = endTime;
        Status = ContractStatus.onGoing;
    }

    public string ContractId { get; }
    public ContractStatus Status { get; private set; }
    public Customer Customer { get; }
    public Room Room { get; }
    public DateTime StartTime { get; }
    public DateTime EndTime { get; }
    public DateTime? CheckInTime { get; private set; }
    public DateTime? CheckOutTime { get; private set; }
    public IReadOnlyList<Utility> UtilitiesUsed => _utilitiesUsed.AsReadOnly();
    public int RoomCapacityUsed { get; private set; }

    public void SetCheckInTime(DateTime checkInTime) => CheckInTime = checkInTime;
    public void SetCheckOutTime(DateTime checkOutTime) => CheckOutTime = checkOutTime;
    public void SetRoomCapacityUsed(int numberOfPeople) => RoomCapacityUsed = numberOfPeople;
    public void EndContract() => Status = ContractStatus.completed;

    public void AddUtility(Utility utility)
    {
        ArgumentNullException.ThrowIfNull(utility);
        _utilitiesUsed.Add(utility);
    }

    public override string ToString() =>
        $"RentalContract{{ContractId='{ContractId}', Customer={Customer.Name}, Room={Room.RoomId}, " +
        $"RoomCapacityUsed={RoomCapacityUsed}, StartTime={StartTime}, EndTime={EndTime}, " +
        $"CheckInTime={CheckInTime}, CheckOutTime={CheckOutTime}, Status={Status}, " +
        $"UtilitiesUsed=[{string.Join(", ", _utilitiesUsed.Select(u => u.UtilityName))}]}}";
}
