using OOP_FINAL_CSharp.Models;
using OOP_FINAL_CSharp.Models.Enums;

namespace OOP_FINAL_CSharp.Services;

public sealed class HotelService
{
    private readonly List<Customer> _customers = [];
    private readonly List<Room> _rooms = [];
    private readonly List<RentalContract> _contracts = [];
    private readonly List<Invoice> _invoices = [];
    private int _invoiceCounter;
    private int _contractCounter;

    public IReadOnlyList<Customer> Customers => _customers.AsReadOnly();
    public IReadOnlyList<Room> Rooms => _rooms.AsReadOnly();
    public IReadOnlyList<RentalContract> Contracts => _contracts.AsReadOnly();
    public IReadOnlyList<Invoice> Invoices => _invoices.AsReadOnly();

    public bool AddCustomer(Customer customer)
    {
        ArgumentNullException.ThrowIfNull(customer);
        _customers.Add(customer);
        return true;
    }

    public Customer? FindCustomer(string customerId) =>
        _customers.FirstOrDefault(c => c.Id == customerId);

    public List<RentalContract> FindOnGoingContractByCustomer(Customer customer) =>
        _contracts.Where(c => c.Customer == customer && c.Status == ContractStatus.onGoing).ToList();

    public RentalContract? FindContractById(string contractId) =>
        _contracts.FirstOrDefault(c => c.ContractId == contractId);

    public bool AddRoom(Room room)
    {
        ArgumentNullException.ThrowIfNull(room);
        _rooms.Add(room);
        return true;
    }

    public Room? FindRoom(string roomId) =>
        _rooms.FirstOrDefault(r => r.RoomId == roomId);

    public bool BookRoom(string roomId, string customerId, DateTime? startTime, DateTime? endTime)
    {
        if (startTime is null || endTime is null)
        {
            Console.WriteLine("Start time or end time cannot be null");
            return false;
        }

        if (startTime.Value >= endTime.Value)
        {
            Console.WriteLine("Invalid start time or end time");
            return false;
        }

        var customer = FindCustomer(customerId);
        var room = FindRoom(roomId);

        if (customer is null)
        {
            Console.WriteLine("Customer not found");
            return false;
        }

        if (room is null)
        {
            Console.WriteLine("Room not found");
            return false;
        }

        if (room.Status != RoomStatus.AVAILABLE)
        {
            Console.WriteLine("Room is already booked");
            return false;
        }

        room.SetRoomStatus(RoomStatus.BOOKED);
        var contract = new RentalContract(
            $"C{_contractCounter++}", customer, room, startTime.Value, endTime.Value);
        _contracts.Add(contract);
        Console.WriteLine("Room booked successfully");
        return true;
    }

    public bool CheckIn(string contractId, DateTime? checkInTime, int numberOfPeople)
    {
        var contract = FindContractById(contractId);
        if (contract is null)
        {
            Console.WriteLine("Contract not found");
            return false;
        }

        if (contract.CheckInTime is not null)
        {
            Console.WriteLine("Customer has already checked in");
            return false;
        }

        if (contract.Status != ContractStatus.onGoing)
        {
            Console.WriteLine("Contract is already completed");
            return false;
        }

        if (checkInTime is null)
        {
            Console.WriteLine("Check-in time cannot be null");
            return false;
        }

        if (checkInTime.Value < contract.StartTime || checkInTime.Value > contract.EndTime)
        {
            Console.WriteLine("Invalid check-in time");
            return false;
        }

        if (numberOfPeople <= 0)
        {
            Console.WriteLine("Number of people must be greater than 0");
            return false;
        }

        contract.SetCheckInTime(checkInTime.Value);
        contract.SetRoomCapacityUsed(numberOfPeople);
        return true;
    }

    public bool UseUtility(string contractId, Utility? utility)
    {
        var contract = FindContractById(contractId);
        if (contract is null)
        {
            Console.WriteLine("Contract not found");
            return false;
        }

        if (contract.Status != ContractStatus.onGoing)
        {
            Console.WriteLine("Contract is not ongoing");
            return false;
        }

        if (contract.CheckInTime is null || contract.CheckOutTime is not null)
        {
            Console.WriteLine("Customer is not checked in");
            return false;
        }

        if (utility is null)
        {
            Console.WriteLine("Utility cannot be null");
            return false;
        }

        contract.AddUtility(utility);
        return true;
    }

    public bool CheckOut(string contractId, DateTime? checkOutTime)
    {
        var contract = FindContractById(contractId);
        if (contract is null)
        {
            Console.WriteLine("Contract not found");
            return false;
        }

        if (contract.CheckInTime is null)
        {
            Console.WriteLine("Customer has not checked in");
            return false;
        }

        if (contract.Status != ContractStatus.onGoing)
        {
            Console.WriteLine("Contract is already completed");
            return false;
        }

        if (checkOutTime is null)
        {
            Console.WriteLine("Check-out time cannot be null");
            return false;
        }

        if (checkOutTime.Value < contract.CheckInTime.Value)
        {
            Console.WriteLine("Invalid check-out time");
            return false;
        }

        contract.SetCheckOutTime(checkOutTime.Value);
        contract.EndContract();
        contract.Room.SetRoomStatus(RoomStatus.AVAILABLE);
        return true;
    }

    public double CalculateRoomFee(RentalContract contract)
    {
        if (contract.CheckOutTime is null)
            return 0;

        var days = (contract.CheckOutTime.Value.Date - contract.StartTime.Date).Days;
        return (double)days * contract.Room.RoomPrice;
    }

    public double CalculateOverstayFee(RentalContract contract)
    {
        if (contract.CheckOutTime is null)
            return 0;

        var days = Math.Max(0, (contract.CheckOutTime.Value.Date - contract.EndTime.Date).Days);
        return contract.Room.RoomGroup.Fee.OverStayFeePerDay * days;
    }

    public double CalculateUtilityFee(RentalContract contract)
    {
        var group = contract.Room.InGroup;
        return contract.UtilitiesUsed
            .Where(u => !u.IsFreeForGroup(group))
            .Sum(u => u.UtilityPrice);
    }

    public double CalculateOverMaxCapacityFee(RentalContract contract)
    {
        if (contract.CheckOutTime is null)
            return 0;

        var maxCapacity = contract.Room.RoomCapacity;
        var capacityUsed = contract.RoomCapacityUsed;
        if (capacityUsed <= maxCapacity)
            return 0;

        return (capacityUsed - maxCapacity) * contract.Room.RoomGroup.Fee.OverMaxCapacityFee;
    }

    public Invoice? CreateInvoice(RentalContract? contract, Customer? payer, PaymentType paymentType)
    {
        if (contract is null)
        {
            Console.WriteLine("Contract not found");
            return null;
        }

        if (contract.CheckOutTime is null)
        {
            Console.WriteLine("Customer has not checked out");
            return null;
        }

        if (_invoices.Any(i => i.Contract == contract))
        {
            Console.WriteLine("Invoice already exists for this contract");
            return null;
        }

        if (payer is null)
            throw new ArgumentNullException(nameof(payer));

        var roomFee = CalculateRoomFee(contract);
        var overstayFee = CalculateOverstayFee(contract);
        var utilityFee = CalculateUtilityFee(contract);
        var overCapacityFee = CalculateOverMaxCapacityFee(contract);
        var deposit = roomFee * 0.10;
        var total = roomFee + overstayFee + utilityFee + overCapacityFee - deposit;

        var invoice = new Invoice(
            $"I{_invoiceCounter++}", payer, contract, paymentType,
            roomFee, overstayFee, overCapacityFee, utilityFee, deposit, total);

        _invoices.Add(invoice);
        Console.WriteLine(invoice);
        return invoice;
    }
}
