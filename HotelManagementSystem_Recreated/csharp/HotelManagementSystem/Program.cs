using System.Globalization;
using HotelManagementSystem.Data;
using HotelManagementSystem.Models;
using HotelManagementSystem.Models.Enums;
using HotelManagementSystem.Models.Policy;
using HotelManagementSystem.Services;

internal static class Program
{
    private static readonly CultureInfo Invariant = CultureInfo.InvariantCulture;
    private static HotelService _hotel = null!;
    private static bool _running = true;

    private static void Main(string[] args)
    {
        if (args.Length > 0 && args[0] == "--self-test")
        {
            RunPersistenceSmokeTest();
            return;
        }
        try
        {
            var repository = new HotelRepository("hotel.db");
            _hotel = new HotelService(repository); // Initializes schema and loads all existing rows into RAM.
            Console.WriteLine($"Database: {repository.DatabasePath}");
            Console.WriteLine("Loaded persisted hotel data successfully.");
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine($"Could not initialize the database: {ex.Message}");
            return;
        }

        Console.WriteLine("==============================================");
        Console.WriteLine("     HOTEL BOOKING SYSTEM - C# + SQLITE");
        Console.WriteLine("==============================================");
        Console.WriteLine("Date/time format: yyyy-MM-dd HH:mm (24-hour)");

        while (_running)
        {
            PrintMenu();
            var choice = ReadInt("Choose an option: ");
            Console.WriteLine();
            try
            {
                switch (choice)
                {
                    case 1: AddCustomer(); break;
                    case 2: ListCustomers(); break;
                    case 3: AddRoomGroup(); break;
                    case 4: AddUtility(); break;
                    case 5: ListRoomGroupsAndUtilities(); break;
                    case 6: AddRoom(); break;
                    case 7: ListRooms(false); break;
                    case 8: ListRooms(true); break;
                    case 9: BookRoom(); break;
                    case 10: ListContracts(); break;
                    case 11: FindOngoingContracts(); break;
                    case 12: CheckIn(); break;
                    case 13: UseUtility(); break;
                    case 14: CheckOut(); break;
                    case 15: PreviewBill(); break;
                    case 16: CreateInvoice(); break;
                    case 17: ListInvoices(); break;
                    case 18: RunReports(); break;
                    case 0: _running = false; Console.WriteLine("Goodbye!"); break;
                    default: Console.WriteLine("Invalid menu option. Choose 0-18."); break;
                }
            }
            catch (Exception ex) when (ex is ArgumentException or InvalidOperationException or Microsoft.Data.Sqlite.SqliteException)
            {
                Console.WriteLine($"Operation failed: {ex.Message}");
            }
            if (_running) { Console.WriteLine(); Console.Write("Press Enter to return to the menu..."); Console.ReadLine(); }
        }
    }


    private static void RunPersistenceSmokeTest()
    {
        var path = Path.Combine(Path.GetTempPath(), $"hotel-smoke-{Guid.NewGuid():N}.db");
        try
        {
            string customerId;
            string contractId;
            {
                var first = new HotelService(new HotelRepository(path));
                var customer = first.CreateCustomer("Test Customer", "000", "TEST-ID", CustomerType.NORMAL);
                var group = first.CreateRoomGroup("Test Group", RoomType.STANDARD, new FeePolicy(25, 10));
                var utility = first.CreateUtility("Laundry", 5);
                first.AssignUtilityToRoomGroup(utility.UtilityId, group.GroupId, false);
                var room = first.CreateRoom("1F", 2, group, RoomStatus.AVAILABLE, 100);
                var start = new DateTime(2026, 10, 20, 10, 0, 0);
                var end = new DateTime(2026, 10, 21, 10, 0, 0);
                if (!first.BookRoom(room.RoomId, customer.Id, start, end)) throw new Exception("Booking smoke test failed.");
                var contract = first.Contracts.Single();
                if (contract.ContractId != "C0") throw new Exception("Contract ID should start at C0 on an empty database.");
                if (!first.CheckIn(contract.ContractId, start, 3)) throw new Exception("Check-in smoke test failed.");
                if (!first.UseUtility(contract.ContractId, utility) || !first.UseUtility(contract.ContractId, utility)) throw new Exception("Utility-use smoke test failed.");
                if (!first.CheckOut(contract.ContractId, new DateTime(2026, 10, 23, 11, 0, 0))) throw new Exception("Check-out smoke test failed.");
                var invoice = first.CreateInvoice(contract, customer, PaymentType.CASH) ?? throw new Exception("Invoice smoke test failed.");
                if (invoice.InvoiceId != "I0" || Math.Abs(invoice.Total - 340) > 0.001) throw new Exception($"Unexpected invoice result: {invoice.Total}.");
                customerId = customer.Id;
                contractId = contract.ContractId;
            }
            {
                var second = new HotelService(new HotelRepository(path));
                if (second.Customers.Count != 1 || second.Contracts.Count != 1 || second.Invoices.Count != 1) throw new Exception("Database reload did not restore all records.");
                var contract = second.FindContractById(contractId) ?? throw new Exception("Contract was not reloaded.");
                if (contract.Customer.Id != customerId || contract.UtilitiesUsed.Count != 2 || contract.Status != ContractStatus.completed) throw new Exception("Contract references/state were not reloaded correctly.");
                if (contract.Room.Status != RoomStatus.AVAILABLE) throw new Exception("Room status was not reloaded correctly.");
                if (second.CreateCustomer("Second Customer", "001", "TEST-ID-2", CustomerType.REGULAR).Id != "KH1") throw new Exception("IDs did not continue from existing rows.");
            }
            Console.WriteLine("PASS: persistence smoke test (CRUD, references, repeated utility usage, billing, restart, IDs).");
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine($"FAIL: {ex.Message}");
            Environment.ExitCode = 1;
        }
        finally
        {
            foreach (var suffix in new[] { "", "-shm", "-wal" })
            {
                try { if (File.Exists(path + suffix)) File.Delete(path + suffix); } catch { }
            }
        }
    }

    private static void PrintMenu()
    {
        Console.WriteLine("\n=============== MAIN MENU ===============");
        Console.WriteLine(" CUSTOMERS"); Console.WriteLine("  1. Add customer"); Console.WriteLine("  2. List customers");
        Console.WriteLine(" ROOM SETUP"); Console.WriteLine("  3. Add room group / category"); Console.WriteLine("  4. Add utility and assign to room groups");
        Console.WriteLine("  5. List room groups and utilities"); Console.WriteLine("  6. Add room");
        Console.WriteLine("  7. List all rooms"); Console.WriteLine("  8. List available rooms only");
        Console.WriteLine(" BOOKING OPERATIONS"); Console.WriteLine("  9. Book a room"); Console.WriteLine(" 10. List contracts");
        Console.WriteLine(" 11. Find customer's ongoing contracts"); Console.WriteLine(" 12. Check in");
        Console.WriteLine(" 13. Use utility"); Console.WriteLine(" 14. Check out");
        Console.WriteLine(" BILLING"); Console.WriteLine(" 15. Preview bill"); Console.WriteLine(" 16. Create invoice"); Console.WriteLine(" 17. List invoices");
        Console.WriteLine(" REPORTS"); Console.WriteLine(" 18. Run all 10 reports (LINQ)"); Console.WriteLine("  0. Exit");
        Console.WriteLine("=========================================");
    }

    private static void AddCustomer()
    {
        var name = ReadNonBlank("Full name: "); var phone = ReadNonBlank("Phone number: ");
        var cccd = ReadNonBlank("CCCD / ID number: "); var type = ReadEnum("Customer type", Enum.GetValues<CustomerType>());
        var customer = _hotel.CreateCustomer(name, phone, cccd, type);
        Console.WriteLine($"Customer saved. ID: {customer.Id}");
    }

    private static void ListCustomers()
    {
        Console.WriteLine("--- Customers ---");
        if (_hotel.Customers.Count == 0) { Console.WriteLine("No customers yet."); return; }
        foreach (var c in _hotel.Customers) Console.WriteLine(c);
    }

    private static void AddRoomGroup()
    {
        var name = ReadNonBlank("Room-group name: "); var type = ReadEnum("Room type", Enum.GetValues<RoomType>());
        var stay = ReadNonNegativeDouble("Overstay fee per day: "); var capacity = ReadNonNegativeDouble("Fee per extra person over capacity: ");
        var group = _hotel.CreateRoomGroup(name, type, new FeePolicy(stay, capacity));
        Console.WriteLine($"Room group saved. ID: {group.GroupId}");
    }

    private static void AddUtility()
    {
        var name = ReadNonBlank("Utility name: "); var price = ReadNonNegativeDouble("Price per use: ");
        var utility = _hotel.CreateUtility(name, price);
        Console.WriteLine($"Utility saved. ID: {utility.UtilityId}");
        if (_hotel.RoomGroups.Count == 0) { Console.WriteLine("Create a room group first to assign this utility later."); return; }
        while (ReadYesNo("Assign utility to a room group? (y/n): "))
        {
            ListRoomGroupsShort(); var id = ReadNonBlank("Room-group ID: ");
            if (_hotel.FindRoomGroup(id) is null) { Console.WriteLine("Room group not found."); continue; }
            var isFree = ReadYesNo("Is it free for this room group? (y/n): ");
            _hotel.AssignUtilityToRoomGroup(utility.UtilityId, id, isFree);
            Console.WriteLine("Utility assignment saved.");
        }
    }

    private static void ListRoomGroupsShort()
    {
        if (_hotel.RoomGroups.Count == 0) { Console.WriteLine("No room groups yet."); return; }
        foreach (var g in _hotel.RoomGroups) Console.WriteLine($"{g.GroupId} | {g.GroupName} | {g.Type}");
    }

    private static void ListRoomGroupsAndUtilities()
    {
        Console.WriteLine("--- Room groups and utilities ---");
        foreach (var g in _hotel.RoomGroups)
        {
            Console.WriteLine($"{g.GroupId} | {g.GroupName} | {g.Type} | overstay/day: {g.Fee.OverStayFeePerDay:F2} | over-capacity/person: {g.Fee.OverMaxCapacityFee:F2}");
            if (g.Utilities.Count == 0) Console.WriteLine("  No utilities assigned.");
            foreach (var u in g.Utilities) Console.WriteLine($"  {u.UtilityId} | {u.UtilityName} | {u.UtilityPrice:F2} | free: {u.IsFreeForGroup(g)}");
        }
        if (_hotel.RoomGroups.Count == 0) Console.WriteLine("No room groups yet.");
    }

    private static void AddRoom()
    {
        if (_hotel.RoomGroups.Count == 0) { Console.WriteLine("Create a room group first (option 3)."); return; }
        var location = ReadNonBlank("Room location / floor: "); var capacity = ReadPositiveInt("Maximum capacity: ");
        var price = ReadNonNegativeDouble("Price per calendar day: "); ListRoomGroupsShort();
        var id = ReadNonBlank("Room-group ID: "); var group = _hotel.FindRoomGroup(id);
        if (group is null) { Console.WriteLine("Room group not found."); return; }
        var room = _hotel.CreateRoom(location, capacity, group, RoomStatus.AVAILABLE, price);
        Console.WriteLine($"Room saved. ID: {room.RoomId}");
    }

    private static void ListRooms(bool availableOnly)
    {
        Console.WriteLine(availableOnly ? "--- Available rooms ---" : "--- All rooms ---");
        var rooms = _hotel.Rooms.Where(r => !availableOnly || r.Status == RoomStatus.AVAILABLE).ToList();
        if (rooms.Count == 0) { Console.WriteLine(availableOnly ? "No rooms currently available." : "No rooms yet."); return; }
        foreach (var r in rooms) Console.WriteLine(r);
    }

    private static void BookRoom()
    {
        if (_hotel.Customers.Count == 0 || _hotel.Rooms.Count == 0) { Console.WriteLine("Add a customer and a room first."); return; }
        ListRooms(true); var roomId = ReadNonBlank("Room ID: "); var customerId = ReadNonBlank("Customer ID: ");
        var start = ReadDateTime("Booking start (yyyy-MM-dd HH:mm): "); var end = ReadDateTime("Booking end   (yyyy-MM-dd HH:mm): ");
        if (!_hotel.BookRoom(roomId, customerId, start, end)) { Console.WriteLine("Booking failed. Check IDs, dates, and room availability."); return; }
        Console.WriteLine($"Booking saved. Contract ID: {_hotel.Contracts[^1].ContractId}");
    }

    private static void ListContracts()
    {
        Console.WriteLine("--- Contracts ---");
        if (_hotel.Contracts.Count == 0) { Console.WriteLine("No contracts yet."); return; }
        foreach (var c in _hotel.Contracts)
            Console.WriteLine($"{c.ContractId} | customer: {c.Customer.Id} ({c.Customer.Name}) | room: {c.Room.RoomId} ({c.Room.RoomType}) | {c.StartTime:yyyy-MM-dd HH:mm} -> {c.EndTime:yyyy-MM-dd HH:mm} | status: {c.Status} | check-in: {FormatDate(c.CheckInTime)} | check-out: {FormatDate(c.CheckOutTime)}");
    }

    private static RentalContract? PromptContract()
    {
        var id = ReadNonBlank("Contract ID: "); var c = _hotel.FindContractById(id);
        if (c is null) Console.WriteLine("Contract not found.");
        return c;
    }

    private static void FindOngoingContracts()
    {
        var id = ReadNonBlank("Customer ID: "); var customer = _hotel.FindCustomer(id);
        if (customer is null) { Console.WriteLine("Customer not found."); return; }
        var result = _hotel.FindOnGoingContractByCustomer(customer);
        if (result.Count == 0) Console.WriteLine("This customer has no ongoing contracts.");
        else result.ForEach(Console.WriteLine);
    }

    private static void CheckIn()
    {
        var c = PromptContract(); if (c is null) return;
        var time = ReadDateTime("Check-in time (yyyy-MM-dd HH:mm): "); var people = ReadPositiveInt("Number of people: ");
        Console.WriteLine(_hotel.CheckIn(c.ContractId, time, people) ? "Check-in saved." : "Check-in failed. Validate contract, time and people count.");
    }

    private static void UseUtility()
    {
        var c = PromptContract(); if (c is null) return;
        if (_hotel.Utilities.Count == 0) { Console.WriteLine("No utilities yet."); return; }
        foreach (var u in _hotel.Utilities) Console.WriteLine(u);
        var id = ReadNonBlank("Utility ID: "); var utility = _hotel.FindUtility(id);
        Console.WriteLine(_hotel.UseUtility(c.ContractId, utility) ? "Utility use saved." : "Could not record utility use.");
    }

    private static void CheckOut()
    {
        var c = PromptContract(); if (c is null) return;
        var time = ReadDateTime("Check-out time (yyyy-MM-dd HH:mm): ");
        Console.WriteLine(_hotel.CheckOut(c.ContractId, time) ? "Check-out saved; room is now available." : "Check-out failed. Validate check-in and time.");
    }

    private static void PreviewBill()
    {
        var c = PromptContract(); if (c is null) return;
        if (c.CheckOutTime is null) { Console.WriteLine("A final bill is available after check-out."); return; }
        PrintBill(c);
    }

    private static double PrintBill(RentalContract c)
    {
        var room = _hotel.CalculateRoomFee(c); var stay = _hotel.CalculateOverstayFee(c); var utility = _hotel.CalculateUtilityFee(c);
        var cap = _hotel.CalculateOverMaxCapacityFee(c); var deposit = room * 0.10; var total = room + stay + utility + cap - deposit;
        Console.WriteLine($"Room fee:                 {room:F2}"); Console.WriteLine($"Overstay fee:             {stay:F2}");
        Console.WriteLine($"Utility fee:              {utility:F2}"); Console.WriteLine($"Over-capacity fee:        {cap:F2}");
        Console.WriteLine($"Deposit deduction (10%): -{deposit:F2}"); Console.WriteLine($"TOTAL DUE:                {total:F2}"); return total;
    }

    private static void CreateInvoice()
    {
        var c = PromptContract(); if (c is null) return;
        if (c.CheckOutTime is null) { Console.WriteLine("Customer must check out first."); return; }
        if (_hotel.Invoices.Any(i => i.Contract.ContractId == c.ContractId)) { Console.WriteLine("Invoice already exists for this contract."); return; }
        ListCustomers(); var payerId = ReadNonBlank("Payer customer ID: "); var payer = _hotel.FindCustomer(payerId);
        if (payer is null) { Console.WriteLine("Payer not found."); return; }
        var method = ReadEnum("Payment method", Enum.GetValues<PaymentType>()); var invoice = _hotel.CreateInvoice(c, payer, method);
        Console.WriteLine(invoice is null ? "Invoice was not created." : $"Invoice saved: {invoice}");
    }

    private static void ListInvoices()
    {
        Console.WriteLine("--- Invoices ---");
        if (_hotel.Invoices.Count == 0) { Console.WriteLine("No invoices yet."); return; }
        foreach (var i in _hotel.Invoices) Console.WriteLine(i);
    }

    private static void RunReports()
    {
        Console.WriteLine("========================================\n             HOTEL REPORTS\n========================================");
        var types = Enum.GetValues<RoomType>(); var invoices = _hotel.Invoices;
        Console.WriteLine("\n1. Available rooms by room type");
        foreach (var t in types) Console.WriteLine($"  {t,-12}: {_hotel.Rooms.Count(r => r.Status == RoomStatus.AVAILABLE && r.RoomType == t)} room(s)");

        Console.WriteLine("\n2. Most booked room type");
        var booked = types.ToDictionary(t => t, t => _hotel.Contracts.LongCount(c => c.Room.RoomType == t));
        var max = booked.Values.DefaultIfEmpty(0).Max();
        if (max == 0) Console.WriteLine("  No bookings yet."); else foreach (var t in types.Where(t => booked[t] == max)) Console.WriteLine($"  {t}: {max} booking(s)");

        Console.WriteLine("\n3. Number of customer return visits");
        var completed = _hotel.Contracts.Where(c => c.Status == ContractStatus.completed).GroupBy(c => c.Customer.Id).ToDictionary(g => g.Key, g => g.Count());
        var returned = completed.Where(x => x.Value > 1).OrderByDescending(x => x.Value).ToList();
        if (returned.Count == 0) Console.WriteLine("  No customer has returned for a second completed stay yet.");
        foreach (var x in returned) Console.WriteLine($"  {x.Key} | completed stays: {x.Value} | return visits: {x.Value - 1}");

        Console.WriteLine("\n4. Revenue by payment method");
        foreach (var p in Enum.GetValues<PaymentType>()) Console.WriteLine($"  {p,-8}: {invoices.Where(i => i.PaymentType == p).Sum(i => i.Total):F2}");

        Console.WriteLine("\n5. Utilities by room group");
        if (_hotel.RoomGroups.Count == 0) Console.WriteLine("  No room groups yet.");
        foreach (var g in _hotel.RoomGroups)
        {
            var listing = g.Utilities.Select(u => $"{u.UtilityName} ({(u.IsFreeForGroup(g) ? "free" : "paid")})").ToList();
            Console.WriteLine($"  {g.GroupId} ({g.Type}): {(listing.Count == 0 ? "no utilities" : string.Join(", ", listing))}");
        }

        Console.WriteLine("\n6. Least booked room type"); var min = booked.Values.DefaultIfEmpty(0).Min();
        foreach (var t in types.Where(t => booked[t] == min)) Console.WriteLine($"  {t}: {min} booking(s)");

        Console.WriteLine("\n7. Revenue contributed by each room type");
        foreach (var t in types) Console.WriteLine($"  {t,-12}: {invoices.Where(i => i.Contract.Room.RoomType == t).Sum(i => i.Total):F2}");

        Console.WriteLine("\n8. Revenue by quarter (based on check-out date)");
        var quarters = invoices.Where(i => i.Contract.CheckOutTime.HasValue).GroupBy(i => $"{i.Contract.CheckOutTime!.Value.Year}-Q{((i.Contract.CheckOutTime.Value.Month - 1) / 3) + 1}").OrderBy(g => g.Key).ToList();
        if (quarters.Count == 0) Console.WriteLine("  No invoices yet.");
        foreach (var q in quarters) Console.WriteLine($"  {q.Key}: {q.Sum(i => i.Total):F2}");

        Console.WriteLine("\n9. Total hotel revenue (created invoices only)"); Console.WriteLine($"  Total: {invoices.Sum(i => i.Total):F2}");

        Console.WriteLine("\n10. Customer with the most return visits");
        var largest = completed.Values.DefaultIfEmpty(0).Max();
        if (largest <= 1) Console.WriteLine("  No returning customer found yet (a return requires at least 2 completed stays).");
        else foreach (var x in completed.Where(x => x.Value == largest))
        {
            var c = _hotel.FindCustomer(x.Key); Console.WriteLine($"  {c?.Name ?? x.Key} ({x.Key}) | completed stays: {x.Value} | return visits: {x.Value - 1}");
        }
    }

    private static string ReadLine(string prompt) { Console.Write(prompt); return Console.ReadLine()?.Trim() ?? string.Empty; }
    private static string ReadNonBlank(string prompt) { while (true) { var s = ReadLine(prompt); if (!string.IsNullOrWhiteSpace(s)) return s; Console.WriteLine("Value cannot be blank."); } }
    private static int ReadInt(string prompt) { while (true) { if (int.TryParse(ReadLine(prompt), out var n)) return n; Console.WriteLine("Please enter a whole number."); } }
    private static int ReadPositiveInt(string prompt) { while (true) { var n = ReadInt(prompt); if (n > 0) return n; Console.WriteLine("Value must be greater than zero."); } }
    private static double ReadNonNegativeDouble(string prompt) { while (true) { if (double.TryParse(ReadLine(prompt), NumberStyles.Float, Invariant, out var n) && double.IsFinite(n) && n >= 0) return n; Console.WriteLine("Enter a valid non-negative number; use a dot for decimals."); } }
    private static bool ReadYesNo(string prompt) { while (true) { var s = ReadLine(prompt).ToLowerInvariant(); if (s is "y" or "yes") return true; if (s is "n" or "no") return false; Console.WriteLine("Enter y or n."); } }
    private static DateTime ReadDateTime(string prompt) { while (true) { if (DateTime.TryParseExact(ReadNonBlank(prompt), "yyyy-MM-dd HH:mm", Invariant, DateTimeStyles.None, out var dt)) return dt; Console.WriteLine("Use yyyy-MM-dd HH:mm, e.g. 2026-10-21 14:30."); } }
    private static T ReadEnum<T>(string prompt, T[] values) where T : struct, Enum { Console.WriteLine(prompt + ":"); for (var i = 0; i < values.Length; i++) Console.WriteLine($"  {i + 1}. {values[i]}"); while (true) { var n = ReadInt("Select number: "); if (n >= 1 && n <= values.Length) return values[n - 1]; Console.WriteLine($"Choose a number from 1 to {values.Length}."); } }
    private static string FormatDate(DateTime? value) => value?.ToString("yyyy-MM-dd HH:mm", Invariant) ?? "-";
}
