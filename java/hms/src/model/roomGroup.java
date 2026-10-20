package model;
import model.enums.*;
import model.policy.*;
import java.util.List;
import java.util.ArrayList;

public class roomGroup {
    private String groupName;
    private String groupId;
    private roomType type;
    private List<utility> utilities;
    private feePolicy fee;
    public roomGroup(String groupName,
                     String groupId,
                     roomType type,
                     feePolicy fee
    ){
        this.groupName = groupName;
        this.groupId = groupId;
        this.type = type;
        this.utilities = new ArrayList<utility>();
        this.fee = fee;
    }

    public roomType getType() {
        return type;
    }

    public List<utility> getUtilities() {
        return utilities;
    }

    public void addUtility(utility utility) {
        utilities.add(utility);
    }

    public feePolicy getFee() {
        return fee;
    }

    public String toString() {
        return "roomGroup{" +
                "groupName='" + groupName + '\'' +
                ", groupId='" + groupId + '\'' +
                ", type=" + type +
                ", utilities=" + utilities +
                ", fee=" + fee +
                '}';
    }
}
