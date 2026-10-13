package model;

import java.util.ArrayList;
import java.util.List;

public class utility {
    String utilityName;
    String utilityId;
    double utilityPrice;
    List<roomGroup> roomGroups; // Free for these group

    public utility(String utilityName, String utilityId, double utilityPrice) {
        this.utilityName = utilityName;
        this.utilityId = utilityId;
        this.utilityPrice = utilityPrice;
        this.roomGroups = new ArrayList<>();    
    }

    public String getUtilityName() {
        return utilityName;
    }

    public String getUtilityId() {
        return utilityId;
    }

    public double getUtilityPrice() {
        return utilityPrice;
    }

    public String toString() {
        return "utility{" +
                "utilityName='" + utilityName + '\'' +
                ", utilityId='" + utilityId + '\'' +
                ", utilityPrice=" + utilityPrice +
                '}';
    }

    public boolean isFreeForGroup(roomGroup group) {
        return roomGroups.contains(group);
    }

    public void addFreeGroup(roomGroup group) {
        if (!roomGroups.contains(group)) {
            roomGroups.add(group);
        }
    }
}
