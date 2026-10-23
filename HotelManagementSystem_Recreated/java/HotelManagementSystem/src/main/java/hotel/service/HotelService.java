package hotel.service;

import hotel.data.HotelRepository;
import hotel.data.HotelState;
import hotel.model.*;
import hotel.model.enums.*;
import hotel.model.policy.FeePolicy;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

/** Business rules and in-memory object graph; accepted changes are saved to SQLite first. */
public final class HotelService {
    private final HotelRepository repository;
    private final List<Customer> customers;
    private final List<RoomGroup> roomGroups;
    private final List<Utility> utilities;
    private final List<Room> rooms;
    private final List<RentalContract> contracts;
    private final List<Invoice> invoices;

    public HotelService(String databaseFile) {
        try {
            repository = new HotelRepository(databaseFile);
            HotelState state = repository.loadState();
            customers = state.customers; roomGroups = state.roomGroups; utilities = state.utilities;
            rooms = state.rooms; contracts = state.contracts; invoices = state.invoices;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not load the hotel database.", ex);
        }
    }

    public HotelRepository getRepository() {
        return repository;
    }
    public java.nio.file.Path getDatabasePath() { return repository.getDatabasePath(); }
    public List<Customer> getCustomers() { return List.copyOf(customers); }
    public List<RoomGroup> getRoomGroups() { return List.copyOf(roomGroups); }
    public List<Utility> getUtilities() { return List.copyOf(utilities); }
    public List<Room> getRooms() { return List.copyOf(rooms); }
    public List<RentalContract> getContracts() { return List.copyOf(contracts); }
    public List<Invoice> getInvoices() { return List.copyOf(invoices); }

    public Customer findCustomer(String id) { return customers.stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null); }
    public RoomGroup findRoomGroup(String id) { return roomGroups.stream().filter(x -> x.getGroupId().equals(id)).findFirst().orElse(null); }
    public Utility findUtility(String id) { return utilities.stream().filter(x -> x.getUtilityId().equals(id)).findFirst().orElse(null); }
    public Room findRoom(String id) { return rooms.stream().filter(x -> x.getRoomId().equals(id)).findFirst().orElse(null); }
    public RentalContract findContractById(String id) { return contracts.stream().filter(x -> x.getContractId().equals(id)).findFirst().orElse(null); }
    public List<RentalContract> findOngoingContractsByCustomer(Customer customer) {
        return contracts.stream().filter(c -> c.getCustomer().getId().equals(customer.getId()) && c.getStatus() == ContractStatus.ON_GOING).toList();
    }

    public Customer createCustomer(String name, String phone, String cccd, CustomerType type) {
        Customer item = new Customer(nextId("KH", customers.stream().map(Customer::getId).toList()), name, phone, cccd, type);
        persist(() -> repository.insertCustomer(item)); customers.add(item); return item;
    }
    public RoomGroup createRoomGroup(String name, RoomType type, FeePolicy fee) {
        RoomGroup item = new RoomGroup(name, nextId("RG", roomGroups.stream().map(RoomGroup::getGroupId).toList()), type, fee);
        persist(() -> repository.insertRoomGroup(item)); roomGroups.add(item); return item;
    }
    public Utility createUtility(String name, double price) {
        Utility item = new Utility(name, nextId("UT", utilities.stream().map(Utility::getUtilityId).toList()), price);
        persist(() -> repository.insertUtility(item)); utilities.add(item); return item;
    }
    public void assignUtilityToRoomGroup(String utilityId, String groupId, boolean free) {
        Utility utility = Objects.requireNonNull(findUtility(utilityId), "Utility not found.");
        RoomGroup group = Objects.requireNonNull(findRoomGroup(groupId), "Room group not found.");
        persist(() -> repository.assignUtility(group, utility, free));
        group.addUtility(utility); if (free) utility.addFreeGroup(group); else utility.removeFreeGroup(group);
    }
    public Room createRoom(String location, int capacity, RoomGroup group, RoomStatus status, double price) {
        if (group == null || roomGroups.stream().noneMatch(g -> g.getGroupId().equals(group.getGroupId()))) throw new IllegalArgumentException("Register the room group first.");
        Room item = new Room(location, nextId("R", rooms.stream().map(Room::getRoomId).toList()), capacity, group, status, price);
        persist(() -> repository.insertRoom(item)); rooms.add(item); return item;
    }

    public boolean bookRoom(String roomId, String customerId, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !start.isBefore(end)) return false;
        Customer customer = findCustomer(customerId); Room room = findRoom(roomId);
        if (customer == null || room == null || room.getRoomStatus() != RoomStatus.AVAILABLE) return false;
        RentalContract contract = new RentalContract(nextId("C", contracts.stream().map(RentalContract::getContractId).toList()), customer, room, start, end);
        persist(() -> repository.insertBooking(contract));
        room.setRoomStatus(RoomStatus.BOOKED); contracts.add(contract); return true;
    }

    public boolean checkIn(String contractId, LocalDateTime time, int people) {
        RentalContract c = findContractById(contractId);
        if (c == null || c.getCheckInTime() != null || c.getStatus() != ContractStatus.ON_GOING || time == null ||
            time.isBefore(c.getStartTime()) || time.isAfter(c.getEndTime()) || people <= 0) return false;
        persist(() -> repository.saveCheckIn(c.getContractId(), time, people));
        c.setCheckInTime(time); c.setRoomCapacityUsed(people); return true;
    }

    public boolean useUtility(String contractId, Utility utility) {
        RentalContract c = findContractById(contractId);
        if (c == null || utility == null || c.getStatus() != ContractStatus.ON_GOING || c.getCheckInTime() == null || c.getCheckOutTime() != null) return false;
        if (utilities.stream().noneMatch(u -> u.getUtilityId().equals(utility.getUtilityId()))) return false;
        persist(() -> repository.addContractUtility(c.getContractId(), utility.getUtilityId()));
        c.addUtility(utility); return true;
    }

    public boolean checkOut(String contractId, LocalDateTime time) {
        RentalContract c = findContractById(contractId);
        if (c == null || c.getCheckInTime() == null || c.getStatus() != ContractStatus.ON_GOING || time == null || time.isBefore(c.getCheckInTime())) return false;
        persist(() -> repository.saveCheckOut(c.getContractId(), time, c.getRoom().getRoomId()));
        c.setCheckOutTime(time); c.endContract(); c.getRoom().setRoomStatus(RoomStatus.AVAILABLE); return true;
    }

    public double calculateRoomFee(RentalContract c) {
        if (c.getCheckOutTime() == null) return 0;
        long days = java.time.temporal.ChronoUnit.DAYS.between(c.getStartTime().toLocalDate(), c.getCheckOutTime().toLocalDate());
        return days * c.getRoom().getRoomPrice();
    }
    public double calculateOverstayFee(RentalContract c) {
        if (c.getCheckOutTime() == null) return 0;
        long days = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(c.getEndTime().toLocalDate(), c.getCheckOutTime().toLocalDate()));
        return days * c.getRoom().getRoomGroup().getFee().overstayFeePerDay();
    }
    public double calculateUtilityFee(RentalContract c) {
        return c.getUtilitiesUsed().stream().filter(u -> !u.isFreeForGroup(c.getRoom().getRoomGroup())).mapToDouble(Utility::getUtilityPrice).sum();
    }
    public double calculateOverCapacityFee(RentalContract c) {
        if (c.getCheckOutTime() == null || c.getRoomCapacityUsed() <= c.getRoom().getRoomCapacity()) return 0;
        return (c.getRoomCapacityUsed() - c.getRoom().getRoomCapacity()) * c.getRoom().getRoomGroup().getFee().overMaxCapacityFee();
    }
    public Invoice createInvoice(String contractId, String payerId, PaymentType method) {
        RentalContract c = findContractById(contractId); Customer payer = findCustomer(payerId);
        if (c == null || payer == null || c.getCheckOutTime() == null || invoices.stream().anyMatch(i -> i.getContract().getContractId().equals(contractId))) return null;
        double room = calculateRoomFee(c), overstay = calculateOverstayFee(c), utility = calculateUtilityFee(c), capacity = calculateOverCapacityFee(c);
        double deposit = room * 0.10, total = room + overstay + utility + capacity - deposit;
        Invoice invoice = new Invoice(nextId("I", invoices.stream().map(Invoice::getInvoiceId).toList()), payer, c, method, room, overstay, capacity, utility, deposit, total);
        persist(() -> repository.insertInvoice(invoice)); invoices.add(invoice); return invoice;
    }

    private static String nextId(String prefix, List<String> existingIds) {
        int max = existingIds.stream().map(id -> id.startsWith(prefix) ? id.substring(prefix.length()) : "")
            .mapToInt(s -> { try { return Integer.parseInt(s); } catch (NumberFormatException ex) { return -1; } }).max().orElse(-1);
        return prefix + (max + 1); // Empty databases start at suffix 0; loaded IDs drive all subsequent values.
    }

    @FunctionalInterface private interface SqlAction { void run() throws SQLException; }
    private static void persist(SqlAction action) {
        try { action.run(); }
        catch (SQLException ex) { throw new IllegalStateException("Database operation failed: " + ex.getMessage(), ex); }
    }

    


}
