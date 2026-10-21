using OOP_FINAL_CSharp.Models.Enums;

namespace OOP_FINAL_CSharp.Models;

public sealed class Room
{
    public Room(
        string roomLocation,
        string roomId,
        int roomCapacity,
        RoomGroup inGroup,
        RoomStatus status,
        double roomPrice)
    {
        RoomLocation = roomLocation;
        RoomId = roomId;
        RoomCapacity = roomCapacity;
        InGroup = inGroup ?? throw new ArgumentNullException(nameof(inGroup));
        Status = status;
        RoomPrice = roomPrice;
    }

    public string RoomLocation { get; }
    public string RoomId { get; }
    public int RoomCapacity { get; }
    public RoomGroup InGroup { get; }
    public RoomStatus Status { get; private set; }
    public double RoomPrice { get; }
    public RoomType RoomType => InGroup.Type;
    public RoomGroup RoomGroup => InGroup;

    public void SetRoomStatus(RoomStatus status) => Status = status;

    public override string ToString() =>
        $"Room{{RoomLocation='{RoomLocation}', RoomId='{RoomId}', RoomCapacity={RoomCapacity}, " +
        $"InGroup={InGroup.GroupName}, Status={Status}, RoomPrice={RoomPrice}}}";
}
