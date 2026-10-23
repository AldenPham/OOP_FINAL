package hotel.model;

import hotel.model.enums.RoomType;
import hotel.model.policy.FeePolicy;
import java.util.ArrayList;
import java.util.List;

public final class RoomGroup {
    private final String groupName;
    private final String groupId;
    private final RoomType type;
    private final FeePolicy fee;
    private final List<Utility> utilities = new ArrayList<>();

    public RoomGroup(String groupName, String groupId, RoomType type, FeePolicy fee) {
        this.groupName = Person.requireText(groupName, "groupName");
        this.groupId = Person.requireText(groupId, "groupId");
        this.type = type;
        this.fee = fee;
    }
    public String getGroupName() { return groupName; }
    public String getGroupId() { return groupId; }
    public RoomType getType() { return type; }
    public FeePolicy getFee() { return fee; }
    public List<Utility> getUtilities() { return List.copyOf(utilities); }
    public void addUtility(Utility utility) {
        if (utility == null) throw new IllegalArgumentException("Utility cannot be null.");
        if (utilities.stream().noneMatch(u -> u.getUtilityId().equals(utility.getUtilityId()))) utilities.add(utility);
    }
    @Override public String toString() { return groupId + " | " + groupName + " | " + type; }
}
