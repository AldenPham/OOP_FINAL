package hotel.model;

import hotel.model.enums.CustomerType;

public final class Customer extends Person {
    private final String cccd;
    private final CustomerType type;
    private int timeStayed;
    private int moneySpended;

    public Customer(String id, String name, String phoneNumber, String cccd, CustomerType type) {
        super(id, name, phoneNumber);
        this.cccd = cccd == null ? "" : cccd.trim();
        this.type = type;
    }
    public String getCccd() { return cccd; }
    public CustomerType getType() { return type; }
    public int getTimeStayed() { return timeStayed; }
    public int getMoneySpended() { return moneySpended; }
    public void setTimeStayed(int value) { timeStayed = value; }
    public void setMoneySpended(int value) { moneySpended = value; }
    @Override public String getRole() { return "Customer"; }
    @Override public String toString() { return super.toString() + " | type: " + type + " | CCCD: " + cccd; }
}
