package hotel.data;

import hotel.model.*;
import hotel.model.enums.*;
import hotel.model.policy.FeePolicy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** SQLite persistence boundary. Multi-table booking/check-out mutations are transactional. */
public final class HotelRepository {
    private final String url;
    private final Path databasePath;

    public HotelRepository(String databaseFile) {
        try {
            databasePath = Path.of(databaseFile).toAbsolutePath().normalize();
            Path parent = databasePath.getParent();
            if (parent != null) Files.createDirectories(parent);
            url = "jdbc:sqlite:" + databasePath;
            initialize();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not initialize SQLite database.", ex);
        }
    }

    public Path getDatabasePath() { return databasePath; }

    private Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) { statement.execute("PRAGMA foreign_keys = ON"); }
        return connection;
    }

    public void initialize() throws SQLException {
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS Customers (
                    Id TEXT PRIMARY KEY, Name TEXT NOT NULL, PhoneNumber TEXT NOT NULL,
                    Cccd TEXT NOT NULL, Type TEXT NOT NULL, TimeStayed INTEGER NOT NULL DEFAULT 0,
                    MoneySpended INTEGER NOT NULL DEFAULT 0
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS RoomGroups (
                    GroupId TEXT PRIMARY KEY, GroupName TEXT NOT NULL, Type TEXT NOT NULL,
                    OverstayFeePerDay REAL NOT NULL DEFAULT 0, OverMaxCapacityFee REAL NOT NULL DEFAULT 0
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS Utilities (
                    UtilityId TEXT PRIMARY KEY, UtilityName TEXT NOT NULL,
                    UtilityPrice REAL NOT NULL CHECK(UtilityPrice >= 0)
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS RoomGroupUtilities (
                    RoomGroupId TEXT NOT NULL, UtilityId TEXT NOT NULL, IsFree INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(RoomGroupId, UtilityId),
                    FOREIGN KEY(RoomGroupId) REFERENCES RoomGroups(GroupId) ON DELETE CASCADE,
                    FOREIGN KEY(UtilityId) REFERENCES Utilities(UtilityId) ON DELETE CASCADE
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS Rooms (
                    RoomId TEXT PRIMARY KEY, RoomLocation TEXT NOT NULL,
                    RoomCapacity INTEGER NOT NULL CHECK(RoomCapacity > 0), RoomGroupId TEXT NOT NULL,
                    Status TEXT NOT NULL, RoomPrice REAL NOT NULL CHECK(RoomPrice >= 0),
                    FOREIGN KEY(RoomGroupId) REFERENCES RoomGroups(GroupId)
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS RentalContracts (
                    ContractId TEXT PRIMARY KEY, CustomerId TEXT NOT NULL, RoomId TEXT NOT NULL,
                    StartTime TEXT NOT NULL, EndTime TEXT NOT NULL, CheckInTime TEXT NULL, CheckOutTime TEXT NULL,
                    Status TEXT NOT NULL, RoomCapacityUsed INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(CustomerId) REFERENCES Customers(Id), FOREIGN KEY(RoomId) REFERENCES Rooms(RoomId),
                    CHECK(StartTime < EndTime)
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ContractUtilities (
                    UsageId INTEGER PRIMARY KEY, ContractId TEXT NOT NULL, UtilityId TEXT NOT NULL,
                    FOREIGN KEY(ContractId) REFERENCES RentalContracts(ContractId) ON DELETE CASCADE,
                    FOREIGN KEY(UtilityId) REFERENCES Utilities(UtilityId)
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS Invoices (
                    InvoiceId TEXT PRIMARY KEY, PayerId TEXT NOT NULL, ContractId TEXT NOT NULL UNIQUE,
                    PaymentType TEXT NOT NULL, RoomFee REAL NOT NULL, OverstayFee REAL NOT NULL,
                    OverMaxCapacityFee REAL NOT NULL, UtilityFee REAL NOT NULL, Deposit REAL NOT NULL, Total REAL NOT NULL,
                    FOREIGN KEY(PayerId) REFERENCES Customers(Id), FOREIGN KEY(ContractId) REFERENCES RentalContracts(ContractId)
                )
                """);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS IX_RentalContracts_CustomerId ON RentalContracts(CustomerId)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS IX_RentalContracts_RoomId ON RentalContracts(RoomId)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS IX_Invoices_PayerId ON Invoices(PayerId)");
        }
    }

    public HotelState loadState() throws SQLException {
        HotelState state = new HotelState();
        Map<String, Customer> customers = new LinkedHashMap<>();
        Map<String, RoomGroup> groups = new LinkedHashMap<>();
        Map<String, Utility> utilities = new LinkedHashMap<>();
        Map<String, Room> rooms = new LinkedHashMap<>();
        Map<String, RentalContract> contracts = new LinkedHashMap<>();
        try (Connection c = open(); Statement s = c.createStatement()) {
            try (ResultSet rs = s.executeQuery("SELECT Id,Name,PhoneNumber,Cccd,Type,TimeStayed,MoneySpended FROM Customers ORDER BY Id")) {
                while (rs.next()) {
                    Customer item = new Customer(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), CustomerType.valueOf(rs.getString(5)));
                    item.setTimeStayed(rs.getInt(6)); item.setMoneySpended(rs.getInt(7));
                    customers.put(item.getId(), item); state.customers.add(item);
                }
            }
            try (ResultSet rs = s.executeQuery("SELECT GroupId,GroupName,Type,OverstayFeePerDay,OverMaxCapacityFee FROM RoomGroups ORDER BY GroupId")) {
                while (rs.next()) {
                    RoomGroup item = new RoomGroup(rs.getString(2), rs.getString(1), RoomType.valueOf(rs.getString(3)), new FeePolicy(rs.getDouble(4), rs.getDouble(5)));
                    groups.put(item.getGroupId(), item); state.roomGroups.add(item);
                }
            }
            try (ResultSet rs = s.executeQuery("SELECT UtilityId,UtilityName,UtilityPrice FROM Utilities ORDER BY UtilityId")) {
                while (rs.next()) {
                    Utility item = new Utility(rs.getString(2), rs.getString(1), rs.getDouble(3));
                    utilities.put(item.getUtilityId(), item); state.utilities.add(item);
                }
            }
            try (ResultSet rs = s.executeQuery("SELECT RoomGroupId,UtilityId,IsFree FROM RoomGroupUtilities ORDER BY RoomGroupId,UtilityId")) {
                while (rs.next()) {
                    RoomGroup group = groups.get(rs.getString(1)); Utility utility = utilities.get(rs.getString(2));
                    if (group != null && utility != null) { group.addUtility(utility); if (rs.getInt(3) != 0) utility.addFreeGroup(group); }
                }
            }
            try (ResultSet rs = s.executeQuery("SELECT RoomId,RoomLocation,RoomCapacity,RoomGroupId,Status,RoomPrice FROM Rooms ORDER BY rowid")) {
                while (rs.next()) {
                    Room item = new Room(rs.getString(2), rs.getString(1), rs.getInt(3), groups.get(rs.getString(4)), RoomStatus.valueOf(rs.getString(5)), rs.getDouble(6));
                    rooms.put(item.getRoomId(), item); state.rooms.add(item);
                }
            }
            try (ResultSet rs = s.executeQuery("SELECT ContractId,CustomerId,RoomId,StartTime,EndTime,CheckInTime,CheckOutTime,Status,RoomCapacityUsed FROM RentalContracts ORDER BY rowid")) {
                while (rs.next()) {
                    RentalContract item = new RentalContract(rs.getString(1), customers.get(rs.getString(2)), rooms.get(rs.getString(3)),
                        LocalDateTime.parse(rs.getString(4)), LocalDateTime.parse(rs.getString(5)));
                    if (rs.getString(6) != null) item.setCheckInTime(LocalDateTime.parse(rs.getString(6)));
                    if (rs.getString(7) != null) item.setCheckOutTime(LocalDateTime.parse(rs.getString(7)));
                    item.setStatus(ContractStatus.valueOf(rs.getString(8))); item.setRoomCapacityUsed(rs.getInt(9));
                    contracts.put(item.getContractId(), item); state.contracts.add(item);
                }
            }
            try (ResultSet rs = s.executeQuery("SELECT ContractId,UtilityId FROM ContractUtilities ORDER BY UsageId")) {
                while (rs.next()) {
                    RentalContract contract = contracts.get(rs.getString(1)); Utility utility = utilities.get(rs.getString(2));
                    if (contract != null && utility != null) contract.addUtility(utility);
                }
            }
            try (ResultSet rs = s.executeQuery("SELECT InvoiceId,PayerId,ContractId,PaymentType,RoomFee,OverstayFee,OverMaxCapacityFee,UtilityFee,Deposit,Total FROM Invoices ORDER BY rowid")) {
                while (rs.next()) {
                    Invoice item = new Invoice(rs.getString(1), customers.get(rs.getString(2)), contracts.get(rs.getString(3)), PaymentType.valueOf(rs.getString(4)),
                        rs.getDouble(5), rs.getDouble(6), rs.getDouble(7), rs.getDouble(8), rs.getDouble(9), rs.getDouble(10));
                    state.invoices.add(item);
                }
            }
        }
        return state;
    }

    public void insertCustomer(Customer x) throws SQLException { execute("INSERT INTO Customers(Id,Name,PhoneNumber,Cccd,Type,TimeStayed,MoneySpended) VALUES(?,?,?,?,?,?,?)", x.getId(), x.getName(), x.getPhoneNumber(), x.getCccd(), x.getType().name(), x.getTimeStayed(), x.getMoneySpended()); }
    public void insertRoomGroup(RoomGroup x) throws SQLException { execute("INSERT INTO RoomGroups(GroupId,GroupName,Type,OverstayFeePerDay,OverMaxCapacityFee) VALUES(?,?,?,?,?)", x.getGroupId(), x.getGroupName(), x.getType().name(), x.getFee().overstayFeePerDay(), x.getFee().overMaxCapacityFee()); }
    public void insertUtility(Utility x) throws SQLException { execute("INSERT INTO Utilities(UtilityId,UtilityName,UtilityPrice) VALUES(?,?,?)", x.getUtilityId(), x.getUtilityName(), x.getUtilityPrice()); }
    public void insertRoom(Room x) throws SQLException { execute("INSERT INTO Rooms(RoomId,RoomLocation,RoomCapacity,RoomGroupId,Status,RoomPrice) VALUES(?,?,?,?,?,?)", x.getRoomId(), x.getRoomLocation(), x.getRoomCapacity(), x.getRoomGroup().getGroupId(), x.getRoomStatus().name(), x.getRoomPrice()); }
    public void assignUtility(RoomGroup group, Utility utility, boolean free) throws SQLException {
        execute("INSERT INTO RoomGroupUtilities(RoomGroupId,UtilityId,IsFree) VALUES(?,?,?) ON CONFLICT(RoomGroupId,UtilityId) DO UPDATE SET IsFree=excluded.IsFree", group.getGroupId(), utility.getUtilityId(), free ? 1 : 0);
    }

    public void insertBooking(RentalContract contract) throws SQLException {
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement update = c.prepareStatement("UPDATE Rooms SET Status=? WHERE RoomId=? AND Status=?")) {
                    update.setString(1, RoomStatus.BOOKED.name()); update.setString(2, contract.getRoom().getRoomId()); update.setString(3, RoomStatus.AVAILABLE.name());
                    if (update.executeUpdate() != 1) throw new SQLException("Room is no longer available in the database.");
                }
                try (PreparedStatement insert = c.prepareStatement("INSERT INTO RentalContracts(ContractId,CustomerId,RoomId,StartTime,EndTime,Status,RoomCapacityUsed) VALUES(?,?,?,?,?,?,0)")) {
                    insert.setString(1, contract.getContractId()); insert.setString(2, contract.getCustomer().getId()); insert.setString(3, contract.getRoom().getRoomId());
                    insert.setString(4, contract.getStartTime().toString()); insert.setString(5, contract.getEndTime().toString()); insert.setString(6, contract.getStatus().name()); insert.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException ex) {
                try { c.rollback(); } catch (SQLException rollbackFailure) { ex.addSuppressed(rollbackFailure); }
                throw ex;
            }
        }
    }

    public void saveCheckIn(String id, LocalDateTime time, int capacity) throws SQLException {
        if (execute("UPDATE RentalContracts SET CheckInTime=?,RoomCapacityUsed=? WHERE ContractId=?", time.toString(), capacity, id) != 1)
            throw new SQLException("Contract could not be saved during check-in.");
    }
    public void addContractUtility(String contractId, String utilityId) throws SQLException { execute("INSERT INTO ContractUtilities(ContractId,UtilityId) VALUES(?,?)", contractId, utilityId); }

    public void saveCheckOut(String contractId, LocalDateTime time, String roomId) throws SQLException {
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement update = c.prepareStatement("UPDATE RentalContracts SET CheckOutTime=?,Status=? WHERE ContractId=?")) {
                    update.setString(1, time.toString()); update.setString(2, ContractStatus.COMPLETED.name()); update.setString(3, contractId);
                    if (update.executeUpdate() != 1) throw new SQLException("Contract could not be saved.");
                }
                try (PreparedStatement updateRoom = c.prepareStatement("UPDATE Rooms SET Status=? WHERE RoomId=?")) {
                    updateRoom.setString(1, RoomStatus.AVAILABLE.name()); updateRoom.setString(2, roomId);
                    if (updateRoom.executeUpdate() != 1) throw new SQLException("Room status could not be saved.");
                }
                c.commit();
            } catch (SQLException | RuntimeException ex) {
                try { c.rollback(); } catch (SQLException rollbackFailure) { ex.addSuppressed(rollbackFailure); }
                throw ex;
            }
        }
    }

    public void insertInvoice(Invoice x) throws SQLException { execute("INSERT INTO Invoices(InvoiceId,PayerId,ContractId,PaymentType,RoomFee,OverstayFee,OverMaxCapacityFee,UtilityFee,Deposit,Total) VALUES(?,?,?,?,?,?,?,?,?,?)", x.getInvoiceId(), x.getPayer().getId(), x.getContract().getContractId(), x.getPaymentType().name(), x.getRoomFee(), x.getOverstayFee(), x.getOverMaxCapacityFee(), x.getUtilityFee(), x.getDeposit(), x.getTotal()); }

    private int execute(String sql, Object... values) throws SQLException {
        try (Connection c = open(); PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) p.setObject(i + 1, values[i]);
            return p.executeUpdate();
        }
    }

    



}
