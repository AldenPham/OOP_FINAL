package model;
import model.enums.*;
import model.enums.roomStatus;

public class Room {
    private String roomLocation;
    private String roomId;
    private int roomCapacity;
    private roomGroup inGroup;
    private roomStatus status;
    private double roomPrice;

    public Room(String roomLocation,
                String roomId,
                int roomCapacity,
                roomGroup inGroup,
                roomStatus status,
                double roomPrice
    ){
        this.roomLocation = roomLocation;
        this.roomId = roomId;
        this.roomCapacity = roomCapacity;
        this.inGroup = inGroup;
        this.status = status;
    }

    public String getRoomId(){
        return roomId;
    }

    public roomStatus getRoomStatus(){
        return status;
    }

    public void setRoomStatus(roomStatus status){
        this.status = status;
    }

    public double getRoomPrice(){
        return roomPrice;
    }

    public roomType getRoomType(){
        return inGroup.getType();
    }

    public roomGroup getRoomGroup(){
        return inGroup;
    }   

    public String toString() {
        return "Room{" +
                "roomLocation='" + roomLocation + '\'' +
                ", roomId='" + roomId + '\'' +
                ", roomCapacity=" + roomCapacity +
                ", inGroup=" + inGroup +
                ", status=" + status +
                ", roomPrice=" + roomPrice +
                '}';
    }

    public roomGroup getInGroup() {
        return inGroup;
    }

    public int getRoomCapacity() {
        return roomCapacity;
    }
}
