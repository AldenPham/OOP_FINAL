package model;

import model.enums.*;

public class Invoice {

    private String invoiceId;

    // Who is paying
    private Customer payer;

    // What rental this invoice is for
    private RentalContract contract;

    private paymentType paymentType;

    private double roomFee;
    private double overstayFee;
    private double utilityFee;
    private double overMaxCapacityFee;

    private double deposit;
    private double total;


    public Invoice(String invoiceId,
                   Customer payer,
                   RentalContract contract,
                   paymentType paymentType,
                   double roomFee,
                   double overstayFee,
                   double overMaxCapacityFee,
                   double utilityFee,
                   double deposit,
                   double total
    ){
        this.invoiceId = invoiceId;
        this.payer = payer;
        this.contract = contract;
        this.paymentType = paymentType;
        this.roomFee = roomFee;
        this.overstayFee = overstayFee;
        this.overMaxCapacityFee = overMaxCapacityFee;
        this.utilityFee = utilityFee;
        this.deposit = deposit;
        this.total = total;
    }

    public paymentType getPaymentType() {
        return paymentType;
    }

    public RentalContract getContract() {
        return contract;
    }
    
    public String toString() {
        return "Invoice{" +
                "invoiceId='" + invoiceId + '\'' +
                ", payer=" + payer.getCustomerName() +
                ", contract=" + contract.getContractId() +
                ", paymentType=" + paymentType +
                ", roomFee=" + roomFee +
                ", overstayFee=" + overstayFee +
                ", utilityFee=" + utilityFee +
                ", overMaxCapacityFee=" + overMaxCapacityFee +
                ", deposit=" + deposit +
                ", total=" + total +
                '}';
    }
}