package service;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

import model.*;
import model.enums.*;
import model.enums.roomStatus;

public class hotelService {
    private List<Customer> customers;
    private List<Room> rooms;
    private List<RentalContract> contracts;
    private List<Invoice> invoices;
    private static int invoiceCounter = 0;
    private static int contractCounter = 0;

    // ===== Constructor
    public hotelService() {
        customers = new ArrayList<>();
        rooms = new ArrayList<>();
        contracts = new ArrayList<>();
        invoices = new ArrayList<>();
    }

    // ===== ADD CUSTOMER
    public boolean addCustomer(Customer customer){

        customers.add(customer);

        return true;
    }

    // ===== FIND CUSTOMER
    public Customer findCustomer(String customerId){
        for(Customer a : customers){
            if(a.get_id().equals(customerId)){
                return a;
            }
        }

        return null;
    }


    // ===== FIND Contract by Customer that is onGoing
    public List<RentalContract> findOnGoingContractByCustomer(Customer customer){
        List<RentalContract> result = new ArrayList<>();
        for(RentalContract c : contracts){
            if(c.getCustomer().equals(customer) && c.getStatus() == contractStatus.onGoing){
                result.add(c);
            }
        }

        return result;
    }

    // ===== FIND Contract by ID
    public RentalContract findContractById(String contractId){
        for(RentalContract c : contracts){
            if(c.getContractId().equals(contractId)){
                return c;
            }
        }   

        return null;
    }


    // ===== ADD ROOM
    public boolean addRoom(Room room){
        rooms.add(room);
        return true;
    }

    // ===== FIND ROOM
    public Room findRoom(String roomId){
        for(Room a : rooms){
            if(a.getRoomId().equals(roomId)){
                return a;
            }
        }

        return null;
    }

    // ===== BOOK ROOM 
    public boolean bookRoom(String roomId, 
                            String customerId,
                            LocalDateTime startTime,
                            LocalDateTime endTime){
        // Check startTime and endTime is valid or no
        if (startTime == null || endTime == null) {
            System.out.println("Start time or end time cannot be null");
            return false;
        }

        if(startTime.isAfter(endTime) || startTime.isEqual(endTime)){
            System.out.println("Invalid start time or end time");
            return false;
        }

        // Find customer and room
        Customer customer = findCustomer(customerId);
        Room room = findRoom(roomId);

        //Check customer and room status
        if(customer == null){
            System.out.println("Customer not found");
            return false;
        }

        if(room == null){
            System.out.println("Room not found");
            return false;
        }

        if(room.getRoomStatus() != roomStatus.AVAILABLE){
            System.out.println("Room is already booked");
            return false;
        }
        else{
            // Change room status to booked
            room.setRoomStatus(roomStatus.BOOKED);
            // Create a new rental contract
            RentalContract contract = new RentalContract(
                "C" + (contractCounter++),
                customer,
                room,
                startTime,
                endTime);

            // Add the contract to the list of contracts
            contracts.add(contract);
            
            System.out.println("Room booked successfully");
        }

        return true;
    }


    // ===== Check IN
    public boolean checkIn(String contractId, LocalDateTime checkInTime){
        RentalContract contract = findContractById(contractId);

        if (contract == null) {
            System.out.println("Contract not found");
            return false;
        }

        if (contract.getCheckInTime() != null) {
            System.out.println("Customer has already checked in");
            return false;
        }   

         if (contract.getStatus() != contractStatus.onGoing) {
            System.out.println("Contract is already completed");
            return false;
        }

        // Check if the check-in time is valid
        if (checkInTime == null) {
            System.out.println("Check-in time cannot be null");
            return false;
        }

        if(checkInTime.isBefore(contract.getStartTime()) || checkInTime.isAfter(contract.getEndTime())){
            System.out.println("Invalid check-in time");
            return false;
        }  

        // Set the check-in time for the contract
        contract.setCheckInTime(checkInTime);

        return true;
    }
    
    // ===== Use Utility
    public boolean useUtility(String contractId, utility utility){
        RentalContract contract = findContractById(contractId);     

        if(contract == null){
            System.out.println("Contract not found");
            return false;
        }

        contract.addUtility(utility);
        return true;
    }

    // ===== Check OUT
    public boolean checkOut(String contractId, LocalDateTime checkOutTime){
        RentalContract contract = findContractById(contractId);

        if (contract == null) {
            System.out.println("Contract not found");
            return false;
        }

        if (contract.getCheckInTime() == null) {
            System.out.println("Customer has not checked in");
            return false;
        }

        if (contract.getStatus() != contractStatus.onGoing) {
            System.out.println("Contract is already completed");
            return false;
        }

        // Check if the check-out time is valid
        if (checkOutTime == null) {
            System.out.println("Check-out time cannot be null");
            return false;
        }

        if(checkOutTime.isBefore(contract.getCheckInTime())){
            System.out.println("Invalid check-out time");
            return false;
        }

        // Set the check-out time for the contract
        contract.setCheckOutTime(checkOutTime);
        contract.endContract();
        contract.getRoom().setRoomStatus(roomStatus.AVAILABLE);

        return true;
    }

    // ===== Calculate bill
    // ====== Calculate room fee
    public double calculateRoomFee(RentalContract contract){
        if (contract.getCheckOutTime() == null) {
            return 0;
        }

        long days = contract.getCheckOutTime().toLocalDate().toEpochDay() - contract.getStartTime().toLocalDate().toEpochDay();

        return (double) days * contract.getRoom().getRoomPrice();
    }

    // ====== Calculate overstay fee
    public double calculateOverstayFee(RentalContract contract){
        if (contract.getCheckOutTime() == null) {
            return 0;
        }

        long days = Math.max(
            0,
            contract.getCheckOutTime().toLocalDate().toEpochDay()
                - contract.getEndTime().toLocalDate().toEpochDay()
        );

        return contract.getOverStayFeePerDay() * days;
    }

    // ====== Calculate utility fee
    public double calculateUtilityFee(RentalContract contract){
        double total = 0.0;
        roomGroup group =contract.getRoom().getInGroup();
        for(utility u : contract.getUtilitiesUsed()){
            if(!u.isFreeForGroup(group)){
                total += u.getUtilityPrice();
            }
        }
        return total;
    }

    // ===== CREATE INVOICE
    public void createInvoice(RentalContract contract,
                             Customer payer,
                             paymentType paymentType) {
        if (contract == null) {
            System.out.println("Contract not found");
            return;
        }

        if (contract.getCheckOutTime() == null) {
            System.out.println("Customer has not checked out");
            return;
        }
        
        for (Invoice invoice : invoices) {
            if (invoice.getContract().equals(contract)) {
                System.out.println("Invoice already exists for this contract");
                return;
            }
        }

        double roomFee = calculateRoomFee(contract);
        double overstayFee = calculateOverstayFee(contract);
        double utilityFee = calculateUtilityFee(contract);

        double deposit = roomFee * 0.10;

        double total = roomFee + overstayFee + utilityFee - deposit;

        Invoice invoice = new Invoice(
            "I" + (invoiceCounter++),
            payer,
            contract,
            paymentType,
            roomFee,
            overstayFee,
            utilityFee,
            deposit,
            total
        );

        invoices.add(invoice);

        System.out.println(invoice.toString());
    }
}



    
