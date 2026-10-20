package model;
import model.enums.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RentalContract {

    private String contractId; 
    private contractStatus status;

    private Customer customer;
    private Room room;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    private List<utility> utilitiesUsed;
    private int roomCapacityUsed = 0;


    //Constructor
    public RentalContract(
            String contractId,
            Customer customer,
            Room room,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        this.contractId = contractId;
        this.customer = customer;
        this.room = room;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = contractStatus.onGoing;
        this.utilitiesUsed = new ArrayList<>();
    }

    // To string
    public String toString(){
        return "RentalContract{" +
                "contractId='" + contractId + '\'' +
                ", customer=" + customer +
                ", room=" + room +
                ", roomCapacityUsed=" + roomCapacityUsed +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status=" + status +
                ", utilitiesUsed=" + utilitiesUsed +
                '}';
    }

    // Getters and Setters
    public Customer getCustomer() {
        return customer;
    }

    public Room getRoom() {
        return room;
    }

    public String getContractId() {
        return contractId;
    }   

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }
    
    public contractStatus getStatus() {
        return status;
    }



    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public void setCheckOutTime(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public void endContract(){
        this.status = contractStatus.completed;
    }

    public void addUtility(utility utility){
        this.utilitiesUsed.add(utility);
    }

    public List<utility> getUtilitiesUsed() {
        return List.copyOf(utilitiesUsed);
    }

    public int getRoomCapacityUsed() {
        return roomCapacityUsed;
    }

    public void setRoomCapacityUsed(int numberOfPeople){
        this.roomCapacityUsed = numberOfPeople;
    }
}