package hotel.model;

import hotel.model.enums.ContractStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class RentalContract {
    private final String contractId;
    private final Customer customer;
    private final Room room;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private ContractStatus status = ContractStatus.ON_GOING;
    private int roomCapacityUsed;
    private final List<Utility> utilitiesUsed = new ArrayList<>();

    public RentalContract(String id, Customer customer, Room room, LocalDateTime start, LocalDateTime end) {
        this.contractId = Person.requireText(id, "contractId");
        this.customer = java.util.Objects.requireNonNull(customer); this.room = java.util.Objects.requireNonNull(room);
        this.startTime = java.util.Objects.requireNonNull(start); this.endTime = java.util.Objects.requireNonNull(end);
        if (!start.isBefore(end)) throw new IllegalArgumentException("Start time must be before end time.");
    }
    public String getContractId() { return contractId; }
    public Customer getCustomer() { return customer; }
    public Room getRoom() { return room; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public ContractStatus getStatus() { return status; }
    public int getRoomCapacityUsed() { return roomCapacityUsed; }
    public List<Utility> getUtilitiesUsed() { return List.copyOf(utilitiesUsed); }
    public void setCheckInTime(LocalDateTime value) { checkInTime = value; }
    public void setCheckOutTime(LocalDateTime value) { checkOutTime = value; }
    public void setRoomCapacityUsed(int value) { roomCapacityUsed = value; }
    public void setStatus(ContractStatus value) { status = value; }
    public void endContract() { status = ContractStatus.COMPLETED; }
    public void addUtility(Utility utility) { utilitiesUsed.add(java.util.Objects.requireNonNull(utility)); }
    @Override public String toString() { return contractId + " | customer: " + customer.getName() + " | room: " + room.getRoomId() + " | " + startTime + " -> " + endTime + " | status: " + status; }
}
