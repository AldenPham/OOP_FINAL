using HotelManagementSystem.Data;
using HotelManagementSystem.Models;
using HotelManagementSystem.Models.Enums;
using HotelManagementSystem.Models.Policy;

namespace HotelManagementSystem.Services;

/// <summary>Business rules and in-memory object graph. Every accepted mutation is persisted before RAM changes.</summary>
public sealed class HotelService
{
    private readonly HotelRepository _repository;
    private readonly List<Customer> _customers;
    private readonly List<RoomGroup> _roomGroups;
    private readonly List<Utility> _utilities;
    private readonly List<Room> _rooms;
    private readonly List<RentalContract> _contracts;
    private readonly List<Invoice> _invoices;

    public HotelService(HotelRepository? repository = null)
    {
        _repository = repository ?? new HotelRepository();
        var state = _repository.LoadState();
        _customers = state.Customers;
        _roomGroups = state.RoomGroups;
        _utilities = state.Utilities;
        _rooms = state.Rooms;
        _contracts = state.Contracts;
        _invoices = state.Invoices;
    }

    public IReadOnlyList<Customer> Customers => _customers.AsReadOnly();
    public IReadOnlyList<RoomGroup> RoomGroups => _roomGroups.AsReadOnly();
    public IReadOnlyList<Utility> Utilities => _utilities.AsReadOnly();
    public IReadOnlyList<Room> Rooms => _rooms.AsReadOnly();
    public IReadOnlyList<RentalContract> Contracts => _contracts.AsReadOnly();
    public IReadOnlyList<Invoice> Invoices => _invoices.AsReadOnly();

    public Customer? FindCustomer(string id) => _customers.FirstOrDefault(x => x.Id == id);
    public RoomGroup? FindRoomGroup(string id) => _roomGroups.FirstOrDefault(x => x.GroupId == id);
    public Utility? FindUtility(string id) => _utilities.FirstOrDefault(x => x.UtilityId == id);
    public Room? FindRoom(string id) => _rooms.FirstOrDefault(x => x.RoomId == id);
    public RentalContract? FindContractById(string id) => _contracts.FirstOrDefault(x => x.ContractId == id);
    public List<RentalContract> FindOnGoingContractByCustomer(Customer customer) =>
        _contracts.Where(c => c.Customer.Id == customer.Id && c.Status == ContractStatus.onGoing).ToList();

    public Customer CreateCustomer(string name, string phoneNumber, string cccd, CustomerType type)
    {
        var item = new Customer(NextId("KH", _customers.Select(x => x.Id)), name, phoneNumber, cccd, type);
        _repository.InsertCustomer(item);
        _customers.Add(item);
        return item;
    }

    public RoomGroup CreateRoomGroup(string name, RoomType type, FeePolicy fee)
    {
        ArgumentNullException.ThrowIfNull(fee);
        var item = new RoomGroup(name, NextId("RG", _roomGroups.Select(x => x.GroupId)), type, fee);
        _repository.InsertRoomGroup(item);
        _roomGroups.Add(item);
        return item;
    }

    public Utility CreateUtility(string name, double price)
    {
        var item = new Utility(name, NextId("UT", _utilities.Select(x => x.UtilityId)), price);
        _repository.InsertUtility(item);
        _utilities.Add(item);
        return item;
    }

    public void AssignUtilityToRoomGroup(string utilityId, string groupId, bool isFree)
    {
        var utility = FindUtility(utilityId) ?? throw new ArgumentException("Utility not found.", nameof(utilityId));
        var group = FindRoomGroup(groupId) ?? throw new ArgumentException("Room group not found.", nameof(groupId));
        _repository.AssignUtility(group, utility, isFree);
        group.AddUtility(utility);
        if (isFree) utility.AddFreeGroup(group);
        // Marking an existing assignment as paid is persisted correctly; the model only needs to remove the free flag.
        else utility.RemoveFreeGroup(group);
    }

    public Room CreateRoom(string location, int capacity, RoomGroup group, RoomStatus status, double price)
    {
        ArgumentNullException.ThrowIfNull(group);
        if (!_roomGroups.Any(g => g.GroupId == group.GroupId)) throw new ArgumentException("Room group must be registered first.", nameof(group));
        var room = new Room(location, NextId("R", _rooms.Select(x => x.RoomId)), capacity, group, status, price);
        _repository.InsertRoom(room);
        _rooms.Add(room);
        return room;
    }

    public bool BookRoom(string roomId, string customerId, DateTime? startTime, DateTime? endTime)
    {
        if (startTime is null || endTime is null || startTime.Value >= endTime.Value) return false;
        var customer = FindCustomer(customerId); var room = FindRoom(roomId);
        if (customer is null || room is null || room.Status != RoomStatus.AVAILABLE) return false;
        var contract = new RentalContract(NextId("C", _contracts.Select(x => x.ContractId)), customer, room, startTime.Value, endTime.Value);
        _repository.InsertBooking(contract); // atomic contract insert + room status update
        room.SetRoomStatus(RoomStatus.BOOKED);
        _contracts.Add(contract);
        return true;
    }

    public bool CheckIn(string contractId, DateTime? checkInTime, int numberOfPeople)
    {
        var contract = FindContractById(contractId);
        if (contract is null || contract.CheckInTime is not null || contract.Status != ContractStatus.onGoing ||
            checkInTime is null || checkInTime.Value < contract.StartTime || checkInTime.Value > contract.EndTime || numberOfPeople <= 0)
            return false;
        _repository.SaveCheckIn(contract.ContractId, checkInTime.Value, numberOfPeople);
        contract.SetCheckInTime(checkInTime.Value); contract.SetRoomCapacityUsed(numberOfPeople);
        return true;
    }

    public bool UseUtility(string contractId, Utility? utility)
    {
        var contract = FindContractById(contractId);
        if (contract is null || utility is null || contract.Status != ContractStatus.onGoing || contract.CheckInTime is null || contract.CheckOutTime is not null)
            return false;
        if (!_utilities.Any(u => u.UtilityId == utility.UtilityId)) return false;
        _repository.AddContractUtility(contract.ContractId, utility.UtilityId);
        contract.AddUtility(utility);
        return true;
    }

    public bool CheckOut(string contractId, DateTime? checkOutTime)
    {
        var contract = FindContractById(contractId);
        if (contract is null || contract.CheckInTime is null || contract.Status != ContractStatus.onGoing ||
            checkOutTime is null || checkOutTime.Value < contract.CheckInTime.Value) return false;
        _repository.SaveCheckOut(contract.ContractId, checkOutTime.Value, contract.Room.RoomId);
        contract.SetCheckOutTime(checkOutTime.Value); contract.EndContract();
        contract.Room.SetRoomStatus(RoomStatus.AVAILABLE);
        return true;
    }

    public double CalculateRoomFee(RentalContract c) => c.CheckOutTime is null ? 0 :
        (c.CheckOutTime.Value.Date - c.StartTime.Date).Days * c.Room.RoomPrice;

    public double CalculateOverstayFee(RentalContract c) => c.CheckOutTime is null ? 0 :
        Math.Max(0, (c.CheckOutTime.Value.Date - c.EndTime.Date).Days) * c.Room.RoomGroup.Fee.OverStayFeePerDay;

    public double CalculateUtilityFee(RentalContract c) => c.UtilitiesUsed
        .Where(u => !u.IsFreeForGroup(c.Room.RoomGroup)).Sum(u => u.UtilityPrice);

    public double CalculateOverMaxCapacityFee(RentalContract c) => c.CheckOutTime is null || c.RoomCapacityUsed <= c.Room.RoomCapacity ? 0 :
        (c.RoomCapacityUsed - c.Room.RoomCapacity) * c.Room.RoomGroup.Fee.OverMaxCapacityFee;

    public Invoice? CreateInvoice(RentalContract? contract, Customer? payer, PaymentType paymentType)
    {
        if (contract is null || payer is null || contract.CheckOutTime is null || _invoices.Any(i => i.Contract.ContractId == contract.ContractId)) return null;
        if (!_customers.Any(c => c.Id == payer.Id)) return null;
        var roomFee = CalculateRoomFee(contract); var overstay = CalculateOverstayFee(contract);
        var utility = CalculateUtilityFee(contract); var capacity = CalculateOverMaxCapacityFee(contract);
        var deposit = roomFee * 0.10; var total = roomFee + overstay + utility + capacity - deposit;
        var invoice = new Invoice(NextId("I", _invoices.Select(x => x.InvoiceId)), payer, contract, paymentType,
            roomFee, overstay, capacity, utility, deposit, total);
        _repository.InsertInvoice(invoice);
        _invoices.Add(invoice);
        return invoice;
    }

    private static string NextId(string prefix, IEnumerable<string> ids)
    {
        var max = ids.Select(id => id.StartsWith(prefix, StringComparison.Ordinal) ? id[prefix.Length..] : string.Empty)
            .Select(s => int.TryParse(s, out var n) ? n : -1).DefaultIfEmpty(-1).Max();
        return $"{prefix}{max + 1}"; // First ID is suffix 0; loaded database IDs are included.
    }
}
