using Microsoft.Data.Sqlite;

namespace OOP_FINAL_CSharp.Data;

public static class Database
{
    private const string ConnectionString = "Data Source=hotel.db";

    public static SqliteConnection GetConnection()
    {
        var connection = new SqliteConnection(ConnectionString);
        connection.Open();
        return connection;
    }

    public static void Initialize()
    {
        using var connection = GetConnection();
        using var command = connection.CreateCommand();

        command.CommandText = """
            PRAGMA foreign_keys = ON;

            CREATE TABLE IF NOT EXISTS Customers (
                Id INTEGER PRIMARY KEY AUTOINCREMENT,
                Name TEXT NOT NULL,
                PhoneNumber TEXT,
                Cccd TEXT,
                Type TEXT
            );

            CREATE TABLE IF NOT EXISTS RoomGroups (
                GroupId INTEGER PRIMARY KEY AUTOINCREMENT,
                GroupName TEXT NOT NULL,
                Type TEXT,
                Fee REAL DEFAULT 0
            );

            CREATE TABLE IF NOT EXISTS Rooms (
                RoomId INTEGER PRIMARY KEY AUTOINCREMENT,
                RoomLocation TEXT NOT NULL,
                RoomCapacity INTEGER NOT NULL,
                RoomGroupId INTEGER,
                Status TEXT NOT NULL,
                RoomPrice REAL NOT NULL,
                FOREIGN KEY (RoomGroupId)
                    REFERENCES RoomGroups(GroupId)
            );

            CREATE TABLE IF NOT EXISTS RentalContracts (
                ContractId INTEGER PRIMARY KEY AUTOINCREMENT,
                CustomerId INTEGER NOT NULL,
                RoomId INTEGER NOT NULL,
                StartTime TEXT NOT NULL,
                EndTime TEXT,
                CheckInTime TEXT,
                CheckOutTime TEXT,
                Status TEXT NOT NULL,
                RoomCapacityUsed INTEGER DEFAULT 1,
                FOREIGN KEY (CustomerId) REFERENCES Customers(Id),
                FOREIGN KEY (RoomId) REFERENCES Rooms(RoomId)
            );

            CREATE TABLE IF NOT EXISTS Utilities (
                UtilityId INTEGER PRIMARY KEY AUTOINCREMENT,
                UtilityName TEXT NOT NULL,
                UtilityPrice REAL NOT NULL
            );

            CREATE TABLE IF NOT EXISTS RoomGroupUtilities (
                RoomGroupId INTEGER NOT NULL,
                UtilityId INTEGER NOT NULL,
                PRIMARY KEY (RoomGroupId, UtilityId),
                FOREIGN KEY (RoomGroupId) REFERENCES RoomGroups(GroupId),
                FOREIGN KEY (UtilityId) REFERENCES Utilities(UtilityId)
            );

            CREATE TABLE IF NOT EXISTS ContractUtilities (
                ContractId INTEGER NOT NULL,
                UtilityId INTEGER NOT NULL,
                Quantity REAL NOT NULL DEFAULT 1,
                PRIMARY KEY (ContractId, UtilityId),
                FOREIGN KEY (ContractId) REFERENCES RentalContracts(ContractId),
                FOREIGN KEY (UtilityId) REFERENCES Utilities(UtilityId)
            );

            CREATE TABLE IF NOT EXISTS Invoices (
                InvoiceId INTEGER PRIMARY KEY AUTOINCREMENT,
                PayerId INTEGER NOT NULL,
                ContractId INTEGER NOT NULL,
                PaymentType TEXT,
                RoomFee REAL NOT NULL DEFAULT 0,
                OverstayFee REAL NOT NULL DEFAULT 0,
                OverMaxCapacityFee REAL NOT NULL DEFAULT 0,
                UtilityFee REAL NOT NULL DEFAULT 0,
                Deposit REAL NOT NULL DEFAULT 0,
                Total REAL NOT NULL DEFAULT 0,
                FOREIGN KEY (PayerId) REFERENCES Customers(Id),
                FOREIGN KEY (ContractId) REFERENCES RentalContracts(ContractId)
            );
            """;

        command.ExecuteNonQuery();
    }
}