using HotelManagementSystem.Models.Enums;
using HotelManagementSystem.Models.Policy;

namespace HotelManagementSystem.Models;

public sealed class RoomGroup(string groupName, string groupId, RoomType type, FeePolicy fee)
{
    private readonly List<Utility> _utilities = [];
    public string GroupName { get; } = string.IsNullOrWhiteSpace(groupName) ? throw new ArgumentException("Group name cannot be blank.", nameof(groupName)) : groupName.Trim();
    public string GroupId { get; } = string.IsNullOrWhiteSpace(groupId) ? throw new ArgumentException("Group ID cannot be blank.", nameof(groupId)) : groupId.Trim();
    public RoomType Type { get; } = type;
    public FeePolicy Fee { get; } = fee ?? throw new ArgumentNullException(nameof(fee));
    public IReadOnlyList<Utility> Utilities => _utilities.AsReadOnly();

    public void AddUtility(Utility utility)
    {
        ArgumentNullException.ThrowIfNull(utility);
        if (_utilities.All(x => x.UtilityId != utility.UtilityId)) _utilities.Add(utility);
    }

    public override string ToString() => $"{GroupId} | {GroupName} | type: {Type}";
}
