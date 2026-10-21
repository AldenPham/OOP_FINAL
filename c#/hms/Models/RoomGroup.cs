using OOP_FINAL_CSharp.Models.Enums;
using OOP_FINAL_CSharp.Models.Policy;

namespace OOP_FINAL_CSharp.Models;

public sealed class RoomGroup
{
    private readonly List<Utility> _utilities = [];

    public RoomGroup(string groupName, string groupId, RoomType type, FeePolicy fee)
    {
        GroupName = groupName;
        GroupId = groupId;
        Type = type;
        Fee = fee ?? throw new ArgumentNullException(nameof(fee));
    }

    public string GroupName { get; }
    public string GroupId { get; }
    public RoomType Type { get; }
    public IReadOnlyList<Utility> Utilities => _utilities.AsReadOnly();
    public FeePolicy Fee { get; }

    public void AddUtility(Utility utility)
    {
        ArgumentNullException.ThrowIfNull(utility);
        _utilities.Add(utility);
    }

    public override string ToString() =>
        $"RoomGroup{{GroupName='{GroupName}', GroupId='{GroupId}', Type={Type}, " +
        $"Utilities=[{string.Join(", ", _utilities)}], Fee={Fee}}}";
}
