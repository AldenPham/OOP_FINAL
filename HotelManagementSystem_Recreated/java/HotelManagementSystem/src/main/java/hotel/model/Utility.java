package hotel.model;

import java.util.HashSet;
import java.util.Set;

public final class Utility {
    private final String utilityName;
    private final String utilityId;
    private final double utilityPrice;
    private final Set<String> freeForGroupIds = new HashSet<>();

    public Utility(String utilityName, String utilityId, double utilityPrice) {
        this.utilityName = Person.requireText(utilityName, "utilityName");
        this.utilityId = Person.requireText(utilityId, "utilityId");
        if (!Double.isFinite(utilityPrice) || utilityPrice < 0) throw new IllegalArgumentException("Utility price must be non-negative.");
        this.utilityPrice = utilityPrice;
    }
    public String getUtilityName() { return utilityName; }
    public String getUtilityId() { return utilityId; }
    public double getUtilityPrice() { return utilityPrice; }
    public boolean isFreeForGroup(RoomGroup group) { return freeForGroupIds.contains(group.getGroupId()); }
    public Set<String> getFreeForGroupIds() { return Set.copyOf(freeForGroupIds); }
    public void addFreeGroup(RoomGroup group) { freeForGroupIds.add(group.getGroupId()); }
    public void removeFreeGroup(RoomGroup group) { freeForGroupIds.remove(group.getGroupId()); }
    @Override public String toString() { return utilityId + " | " + utilityName + " | " + String.format(java.util.Locale.ROOT, "%.2f", utilityPrice); }
}
