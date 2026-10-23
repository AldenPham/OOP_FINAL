package hotel.model;

public abstract class Person {
    private final String id;
    private final String name;
    private final String phoneNumber;

    protected Person(String id, String name, String phoneNumber) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.phoneNumber = phoneNumber == null ? "" : phoneNumber.trim();
    }

    protected static String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " cannot be blank.");
        return value.trim();
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getPhoneNumber() { return phoneNumber; }
    public abstract String getRole();
    @Override public String toString() { return getId() + " | " + getName() + " | phone: " + getPhoneNumber(); }
}
