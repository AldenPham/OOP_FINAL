using System.Globalization;
using HotelManagementSystem.Models;
using HotelManagementSystem.Models.Enums;
using HotelManagementSystem.Models.Policy;
using Microsoft.Data.Sqlite;

namespace HotelManagementSystem.Data;

/// <summary>SQLite persistence boundary. Each operation owns its connection; multi-table changes use transactions.</summary>
public sealed class HotelRepository
{
    private readonly string _connectionString;

    public HotelRepository(string databasePath = "hotel.db")
    {
        var fullPath = Path.GetFullPath(databasePath);
        Directory.CreateDirectory(Path.GetDirectoryName(fullPath)!);
        _connectionString = new SqliteConnectionStringBuilder { DataSource = fullPath, ForeignKeys = true }.ToString();
        DatabasePath = fullPath;
        Initialize();
    }

    public string DatabasePath { get; }

    private SqliteConnection Open()
    {
        var connection = new SqliteConnection(_connectionString);
        connection.Open();
        using var command = connection.CreateCommand();
        command.CommandText = "PRAGMA foreign_keys = ON;";
        command.ExecuteNonQuery();
        return connection;
    }

    public void Initialize()
    {
        using var connection = Open();
        using var command = connection.CreateCommand();
        command.CommandText = """
            CREATE TABLE IF NOT EXISTS Customers (
                Id TEXT PRIMARY KEY, Name TEXT NOT NULL, PhoneNumber TEXT NOT NULL,
                Cccd TEXT NOT NULL, Type TEXT NOT NULL, TimeStayed INTEGER NOT NULL DEFAULT 0,
                MoneySpended INTEGER NOT NULL DEFAULT 0
            );
            CREATE TABLE IF NOT EXISTS RoomGroups (
                GroupId TEXT PRIMARY KEY, GroupName TEXT NOT NULL, Type TEXT NOT NULL,
                OverstayFeePerDay REAL NOT NULL DEFAULT 0, OverMaxCapacityFee REAL NOT NULL DEFAULT 0
            );
            CREATE TABLE IF NOT EXISTS Utilities (
                UtilityId TEXT PRIMARY KEY, UtilityName TEXT NOT NULL, UtilityPrice REAL NOT NULL CHECK(UtilityPrice >= 0)
            );
            CREATE TABLE IF NOT EXISTS RoomGroupUtilities (
                RoomGroupId TEXT NOT NULL, UtilityId TEXT NOT NULL, IsFree INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(RoomGroupId, UtilityId),
                FOREIGN KEY(RoomGroupId) REFERENCES RoomGroups(GroupId) ON DELETE CASCADE,
                FOREIGN KEY(UtilityId) REFERENCES Utilities(UtilityId) ON DELETE CASCADE
            );
            CREATE TABLE IF NOT EXISTS Rooms (
                RoomId TEXT PRIMARY KEY, RoomLocation TEXT NOT NULL, RoomCapacity INTEGER NOT NULL CHECK(RoomCapacity > 0),
                RoomGroupId TEXT NOT NULL, Status TEXT NOT NULL, RoomPrice REAL NOT NULL CHECK(RoomPrice >= 0),
                FOREIGN KEY(RoomGroupId) REFERENCES RoomGroups(GroupId)
            );
            CREATE TABLE IF NOT EXISTS RentalContracts (
                ContractId TEXT PRIMARY KEY, CustomerId TEXT NOT NULL, RoomId TEXT NOT NULL,
                StartTime TEXT NOT NULL, EndTime TEXT NOT NULL, CheckInTime TEXT NULL, CheckOutTime TEXT NULL,
                Status TEXT NOT NULL, RoomCapacityUsed INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(CustomerId) REFERENCES Customers(Id), FOREIGN KEY(RoomId) REFERENCES Rooms(RoomId),
                CHECK(StartTime < EndTime)
            );
            CREATE TABLE IF NOT EXISTS ContractUtilities (
                UsageId INTEGER PRIMARY KEY, ContractId TEXT NOT NULL, UtilityId TEXT NOT NULL,
                FOREIGN KEY(ContractId) REFERENCES RentalContracts(ContractId) ON DELETE CASCADE,
                FOREIGN KEY(UtilityId) REFERENCES Utilities(UtilityId)
            );
            CREATE TABLE IF NOT EXISTS Invoices (
                InvoiceId TEXT PRIMARY KEY, PayerId TEXT NOT NULL, ContractId TEXT NOT NULL UNIQUE,
                PaymentType TEXT NOT NULL, RoomFee REAL NOT NULL, OverstayFee REAL NOT NULL,
                OverMaxCapacityFee REAL NOT NULL, UtilityFee REAL NOT NULL, Deposit REAL NOT NULL, Total REAL NOT NULL,
                FOREIGN KEY(PayerId) REFERENCES Customers(Id), FOREIGN KEY(ContractId) REFERENCES RentalContracts(ContractId)
            );
            CREATE INDEX IF NOT EXISTS IX_RentalContracts_CustomerId ON RentalContracts(CustomerId);
            CREATE INDEX IF NOT EXISTS IX_RentalContracts_RoomId ON RentalContracts(RoomId);
            CREATE INDEX IF NOT EXISTS IX_Invoices_PayerId ON Invoices(PayerId);
            """;
        command.ExecuteNonQuery();
    }

    public HotelState LoadState()
    {
        using var connection = Open();
        var state = new HotelState();
        var customers = new Dictionary<string, Customer>(StringComparer.Ordinal);
        var groups = new Dictionary<string, RoomGroup>(StringComparer.Ordinal);
        var utilities = new Dictionary<string, Utility>(StringComparer.Ordinal);
        var rooms = new Dictionary<string, Room>(StringComparer.Ordinal);
        var contracts = new Dictionary<string, RentalContract>(StringComparer.Ordinal);

        using (var command = connection.CreateCommand())
        {
            command.CommandText = "SELECT Id, Name, PhoneNumber, Cccd, Type, TimeStayed, MoneySpended FROM Customers ORDER BY Id";
            using var reader = command.ExecuteReader();
            while (reader.Read())
            {
                var customer = new Customer(reader.GetString(0), reader.GetString(1), reader.GetString(2),
                    reader.GetString(3), Enum.Parse<CustomerType>(reader.GetString(4)))
                { TimeStayed = reader.GetInt32(5), MoneySpended = reader.GetInt32(6) };
                customers.Add(customer.Id, customer); state.Customers.Add(customer);
            }
        }
        using (var command = connection.CreateCommand())
        {
            command.CommandText = "SELECT GroupId, GroupName, Type, OverstayFeePerDay, OverMaxCapacityFee FROM RoomGroups ORDER BY GroupId";
            using var reader = command.ExecuteReader();
            while (reader.Read())
            {
                var group = new RoomGroup(reader.GetString(1), reader.GetString(0), Enum.Parse<RoomType>(reader.GetString(2)),
                    new FeePolicy(reader.GetDouble(3), reader.GetDouble(4)));
                groups.Add(group.GroupId, group); state.RoomGroups.Add(group);
            }
        }
        using (var command = connection.CreateCommand())
        {
            command.CommandText = "SELECT UtilityId, UtilityName, UtilityPrice FROM Utilities ORDER BY UtilityId";
            using var reader = command.ExecuteReader();
            while (reader.Read())
            {
                var utility = new Utility(reader.GetString(1), reader.GetString(0), reader.GetDouble(2));
                utilities.Add(utility.UtilityId, utility); state.Utilities.Add(utility);
            }
        }
        using (var command = connection.CreateCommand())
        {
            command.CommandText = "SELECT RoomGroupId, UtilityId, IsFree FROM RoomGroupUtilities ORDER BY RoomGroupId, UtilityId";
            using var reader = command.ExecuteReader();
            while (reader.Read())
            {
                var group = groups[reader.GetString(0)]; var utility = utilities[reader.GetString(1)];
                group.AddUtility(utility);
                if (reader.GetInt32(2) != 0) utility.AddFreeGroup(group);
            }
        }
        using (var command = connection.CreateCommand())
        {
            command.CommandText = "SELECT RoomId, RoomLocation, RoomCapacity, RoomGroupId, Status, RoomPrice FROM Rooms ORDER BY RoomId";
            using var reader = command.ExecuteReader();
            while (reader.Read())
            {
                var room = new Room(reader.GetString(1), reader.GetString(0), reader.GetInt32(2), groups[reader.GetString(3)],
                    Enum.Parse<RoomStatus>(reader.GetString(4)), reader.GetDouble(5));
                rooms.Add(room.RoomId, room); state.Rooms.Add(room);
            }
        }
        using (var command = connection.CreateCommand())
        {
            command.CommandText = "SELECT ContractId, CustomerId, RoomId, StartTime, EndTime, CheckInTime, CheckOutTime, Status, RoomCapacityUsed FROM RentalContracts ORDER BY rowid";
            using var reader = command.ExecuteReader();
            while (reader.Read())
            {
                var contract = new RentalContract(reader.GetString(0), customers[reader.GetString(1)], rooms[reader.GetString(2)],
                    ParseDate(reader.GetString(3)), ParseDate(reader.GetString(4)));
                if (!reader.IsDBNull(5)) contract.SetCheckInTime(ParseDate(reader.GetString(5)));
                if (!reader.IsDBNull(6)) contract.SetCheckOutTime(ParseDate(reader.GetString(6)));
                contract.SetRoomCapacityUsed(reader.GetInt32(8));
                contract.SetStatus(Enum.Parse<ContractStatus>(reader.GetString(7)));
                contracts.Add(contract.ContractId, contract); state.Contracts.Add(contract);
            }
        }
        using (var command = connection.CreateCommand())
        {
            command.CommandText = "SELECT ContractId, UtilityId FROM ContractUtilities ORDER BY UsageId";
            using var reader = command.ExecuteReader();
            while (reader.Read()) contracts[reader.GetString(0)].AddUtility(utilities[reader.GetString(1)]);
        }
        using (var command = connection.CreateCommand())
        {
            command.CommandText = "SELECT InvoiceId, PayerId, ContractId, PaymentType, RoomFee, OverstayFee, OverMaxCapacityFee, UtilityFee, Deposit, Total FROM Invoices ORDER BY rowid";
            using var reader = command.ExecuteReader();
            while (reader.Read())
            {
                var invoice = new Invoice(reader.GetString(0), customers[reader.GetString(1)], contracts[reader.GetString(2)],
                    Enum.Parse<PaymentType>(reader.GetString(3)), reader.GetDouble(4), reader.GetDouble(5), reader.GetDouble(6),
                    reader.GetDouble(7), reader.GetDouble(8), reader.GetDouble(9));
                state.Invoices.Add(invoice);
            }
        }
        return state;
    }

    public void InsertCustomer(Customer c) => Execute("INSERT INTO Customers(Id,Name,PhoneNumber,Cccd,Type,TimeStayed,MoneySpended) VALUES($id,$name,$phone,$cccd,$type,$stayed,$spent)",
        ("$id", c.Id), ("$name", c.Name), ("$phone", c.PhoneNumber), ("$cccd", c.Cccd), ("$type", c.Type.ToString()), ("$stayed", c.TimeStayed), ("$spent", c.MoneySpended));

    public void InsertRoomGroup(RoomGroup g) => Execute("INSERT INTO RoomGroups(GroupId,GroupName,Type,OverstayFeePerDay,OverMaxCapacityFee) VALUES($id,$name,$type,$overstay,$capacity)",
        ("$id", g.GroupId), ("$name", g.GroupName), ("$type", g.Type.ToString()), ("$overstay", g.Fee.OverStayFeePerDay), ("$capacity", g.Fee.OverMaxCapacityFee));

    public void InsertUtility(Utility u) => Execute("INSERT INTO Utilities(UtilityId,UtilityName,UtilityPrice) VALUES($id,$name,$price)",
        ("$id", u.UtilityId), ("$name", u.UtilityName), ("$price", u.UtilityPrice));

    public void InsertRoom(Room r) => Execute("INSERT INTO Rooms(RoomId,RoomLocation,RoomCapacity,RoomGroupId,Status,RoomPrice) VALUES($id,$location,$capacity,$group,$status,$price)",
        ("$id", r.RoomId), ("$location", r.RoomLocation), ("$capacity", r.RoomCapacity), ("$group", r.InGroup.GroupId), ("$status", r.Status.ToString()), ("$price", r.RoomPrice));

    public void AssignUtility(RoomGroup group, Utility utility, bool isFree)
    {
        Execute("INSERT INTO RoomGroupUtilities(RoomGroupId,UtilityId,IsFree) VALUES($group,$utility,$free) ON CONFLICT(RoomGroupId,UtilityId) DO UPDATE SET IsFree=excluded.IsFree",
            ("$group", group.GroupId), ("$utility", utility.UtilityId), ("$free", isFree ? 1 : 0));
    }

    public void InsertBooking(RentalContract contract)
    {
        using var connection = Open(); using var transaction = connection.BeginTransaction();
        using (var update = connection.CreateCommand())
        {
            update.Transaction = transaction; update.CommandText = "UPDATE Rooms SET Status=$booked WHERE RoomId=$room AND Status=$available";
            Add(update, "$booked", RoomStatus.BOOKED.ToString()); Add(update, "$room", contract.Room.RoomId); Add(update, "$available", RoomStatus.AVAILABLE.ToString());
            if (update.ExecuteNonQuery() != 1) throw new InvalidOperationException("The room is no longer available in the database.");
        }
        using (var insert = connection.CreateCommand())
        {
            insert.Transaction = transaction;
            insert.CommandText = "INSERT INTO RentalContracts(ContractId,CustomerId,RoomId,StartTime,EndTime,Status,RoomCapacityUsed) VALUES($id,$customer,$room,$start,$end,$status,0)";
            Add(insert, "$id", contract.ContractId); Add(insert, "$customer", contract.Customer.Id); Add(insert, "$room", contract.Room.RoomId);
            Add(insert, "$start", FormatDate(contract.StartTime)); Add(insert, "$end", FormatDate(contract.EndTime)); Add(insert, "$status", contract.Status.ToString());
            insert.ExecuteNonQuery();
        }
        transaction.Commit();
    }

    public void SaveCheckIn(string contractId, DateTime checkInTime, int capacity)
    {
        if (Execute("UPDATE RentalContracts SET CheckInTime=$time, RoomCapacityUsed=$capacity WHERE ContractId=$id",
                ("$time", FormatDate(checkInTime)), ("$capacity", capacity), ("$id", contractId)) != 1)
            throw new InvalidOperationException("Contract could not be saved during check-in.");
    }

    public void AddContractUtility(string contractId, string utilityId) => Execute(
        "INSERT INTO ContractUtilities(ContractId,UtilityId) VALUES($contract,$utility)", ("$contract", contractId), ("$utility", utilityId));

    public void SaveCheckOut(string contractId, DateTime checkOutTime, string roomId)
    {
        using var connection = Open(); using var transaction = connection.BeginTransaction();
        using (var update = connection.CreateCommand())
        {
            update.Transaction = transaction; update.CommandText = "UPDATE RentalContracts SET CheckOutTime=$time, Status=$status WHERE ContractId=$id";
            Add(update, "$time", FormatDate(checkOutTime)); Add(update, "$status", ContractStatus.completed.ToString()); Add(update, "$id", contractId);
            if (update.ExecuteNonQuery() != 1) throw new InvalidOperationException("Contract could not be saved.");
        }
        using (var updateRoom = connection.CreateCommand())
        {
            updateRoom.Transaction = transaction; updateRoom.CommandText = "UPDATE Rooms SET Status=$status WHERE RoomId=$id";
            Add(updateRoom, "$status", RoomStatus.AVAILABLE.ToString()); Add(updateRoom, "$id", roomId); updateRoom.ExecuteNonQuery();
        }
        transaction.Commit();
    }

    public void InsertInvoice(Invoice i) => Execute("INSERT INTO Invoices(InvoiceId,PayerId,ContractId,PaymentType,RoomFee,OverstayFee,OverMaxCapacityFee,UtilityFee,Deposit,Total) VALUES($id,$payer,$contract,$payment,$room,$stay,$capacity,$utility,$deposit,$total)",
        ("$id", i.InvoiceId), ("$payer", i.Payer.Id), ("$contract", i.Contract.ContractId), ("$payment", i.PaymentType.ToString()),
        ("$room", i.RoomFee), ("$stay", i.OverstayFee), ("$capacity", i.OverMaxCapacityFee), ("$utility", i.UtilityFee), ("$deposit", i.Deposit), ("$total", i.Total));

    private int Execute(string sql, params (string Name, object? Value)[] parameters)
    {
        using var connection = Open(); using var command = connection.CreateCommand(); command.CommandText = sql;
        foreach (var (name, value) in parameters) Add(command, name, value);
        return command.ExecuteNonQuery();
    }

    private static void Add(SqliteCommand command, string name, object? value) =>
        command.Parameters.AddWithValue(name, value ?? DBNull.Value);

    private static string FormatDate(DateTime value) => value.ToString("O", CultureInfo.InvariantCulture);
    private static DateTime ParseDate(string value) => DateTime.Parse(value, CultureInfo.InvariantCulture, DateTimeStyles.RoundtripKind);
}
