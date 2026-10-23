package hotel.model;

import hotel.model.enums.PaymentType;

public final class Invoice {
    private final String invoiceId;
    private final Customer payer;
    private final RentalContract contract;
    private final PaymentType paymentType;
    private final double roomFee, overstayFee, overMaxCapacityFee, utilityFee, deposit, total;

    public Invoice(String id, Customer payer, RentalContract contract, PaymentType paymentType,
                   double roomFee, double overstayFee, double overMaxCapacityFee, double utilityFee, double deposit, double total) {
        this.invoiceId = Person.requireText(id, "invoiceId"); this.payer = java.util.Objects.requireNonNull(payer);
        this.contract = java.util.Objects.requireNonNull(contract); this.paymentType = java.util.Objects.requireNonNull(paymentType);
        this.roomFee = roomFee; this.overstayFee = overstayFee; this.overMaxCapacityFee = overMaxCapacityFee;
        this.utilityFee = utilityFee; this.deposit = deposit; this.total = total;
    }
    public String getInvoiceId() { return invoiceId; }
    public Customer getPayer() { return payer; }
    public RentalContract getContract() { return contract; }
    public PaymentType getPaymentType() { return paymentType; }
    public double getRoomFee() { return roomFee; }
    public double getOverstayFee() { return overstayFee; }
    public double getOverMaxCapacityFee() { return overMaxCapacityFee; }
    public double getUtilityFee() { return utilityFee; }
    public double getDeposit() { return deposit; }
    public double getTotal() { return total; }
    @Override public String toString() { return String.format(java.util.Locale.ROOT, "Invoice %s | contract: %s | payer: %s | payment: %s | total: %.2f", invoiceId, contract.getContractId(), payer.getName(), paymentType, total); }
}
