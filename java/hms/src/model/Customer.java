package model;
import model.enums.*;


public class Customer extends person {
    private String cccd;
    private int timeStayed = 0;
    private int moneySpended = 0;
    private customerType type;

    //Constructor
    public Customer(String id,
                    String name,
                    String phoneNumber,
                    String cccd,
                    customerType type
    ){
        super(id, name, phoneNumber);
        this.cccd = cccd;
        this.type = type;
    }

    public String get_id(){
        return getId();
    }

    public String getCustomerName(){
        return getName();
    }   

    public String toString() {
        return "Customer{" +
                "id='" + getId() + '\'' +
                ", name='" + getName() + '\'' +
                ", phoneNumber='" + getPhoneNumber() + '\'' +
                ", cccd='" + cccd + '\'' +
                ", timeStayed=" + timeStayed +
                ", moneySpended=" + moneySpended +
                ", type=" + type +
                '}';
    }

    @Override 
    public String getRole() {
        return "Customer";
    }
}
