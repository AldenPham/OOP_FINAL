package hotel.model;

import hotel.model.enums.RoomStatus;
import hotel.model.enums.RoomType;

public final class Room {
    private final String roomLocation;
    private final String roomId;
    private final int roomCapacity;
    private final RoomGroup inGroup;
    private RoomStatus status;
    private final double roomPrice;

    public Room(String roomLocation, String roomId, int roomCapacity, RoomGroup group, RoomStatus status, double roomPrice) {
        this.roomLocation = Person.requireText(roomLocation, "roomLocation");
        this.roomId = Person.requireText(roomId, "roomId");
        if (roomCapacity <= 0) throw new IllegalArgumentException("Room capacity must be positive.");
        if (!Double.isFinite(roomPrice) || roomPrice < 0) throw new IllegalArgumentException("Room price must be non-negative.");
        this.roomCapacity = roomCapacity; this.inGroup = java.util.Objects.requireNonNull(group); this.status = status; this.roomPrice = roomPrice;
    }
    public String getRoomLocation() { return roomLocation; }
    public String getRoomId() { return roomId; }
    public int getRoomCapacity() { return roomCapacity; }
    public RoomGroup getInGroup() { return inGroup; }
    public RoomGroup getRoomGroup() { return inGroup; }
    public RoomType getRoomType() { return inGroup.getType(); }
    public RoomStatus getRoomStatus() { return status; }
    public void setRoomStatus(RoomStatus value) { status = value; }
    public double getRoomPrice() { return roomPrice; }
    @Override public String toString() { return roomId + " | type: " + getRoomType() + " | group: " + inGroup.getGroupName() + " | capacity: " + roomCapacity + " | price/day: " + String.format(java.util.Locale.ROOT, "%.2f", roomPrice) + " | status: " + status; }
}
