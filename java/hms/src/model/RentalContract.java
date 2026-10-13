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

    private double overStayFeePerDay;

    private double calOverStayFeePerDay(){
        double basePrice = room.getRoomPrice();
        return basePrice * room.getRoomType().getOverstayMultiplier() * 10; // Example
        //  calculation based on room type

    }

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
        this.overStayFeePerDay = calOverStayFeePerDay();
        this.status = contractStatus.onGoing;
        this.utilitiesUsed = new ArrayList<>();
    }

    // To string
    public String toString(){
        return "RentalContract{" +
                "contractId='" + contractId + '\'' +
                ", customer=" + customer +
                ", room=" + room +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", overStayFeePerDay=" + overStayFeePerDay +
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

    public double getOverStayFeePerDay() {
        return overStayFeePerDay;
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
        return utilitiesUsed;
    }
}