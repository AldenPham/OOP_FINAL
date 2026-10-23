using HotelManagementSystem.Models.Enums;

namespace HotelManagementSystem.Models;

public sealed class Room(string roomLocation, string roomId, int roomCapacity, RoomGroup inGroup, RoomStatus status, double roomPrice)
{
    public string RoomLocation { get; } = string.IsNullOrWhiteSpace(roomLocation) ? throw new ArgumentException("Room location cannot be blank.", nameof(roomLocation)) : roomLocation.Trim();
    public string RoomId { get; } = string.IsNullOrWhiteSpace(roomId) ? throw new ArgumentException("Room ID cannot be blank.", nameof(roomId)) : roomId.Trim();
    public int RoomCapacity { get; } = roomCapacity > 0 ? roomCapacity : throw new ArgumentOutOfRangeException(nameof(roomCapacity));
    public RoomGroup InGroup { get; } = inGroup ?? throw new ArgumentNullException(nameof(inGroup));
    public RoomGroup RoomGroup => InGroup;
    public RoomType RoomType => InGroup.Type;
    public RoomStatus Status { get; private set; } = status;
    public double RoomPrice { get; } = roomPrice >= 0 && double.IsFinite(roomPrice) ? roomPrice : throw new ArgumentOutOfRangeException(nameof(roomPrice));
    public void SetRoomStatus(RoomStatus status) => Status = status;
    public override string ToString() => $"{RoomId} | type: {RoomType} | group: {InGroup.GroupName} | capacity: {RoomCapacity} | price/day: {RoomPrice:F2} | status: {Status}";
}
