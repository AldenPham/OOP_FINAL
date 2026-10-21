namespace OOP_FINAL_CSharp.Models;

public sealed class Utility
{
    private readonly List<RoomGroup> _freeForGroups = [];

    public Utility(string utilityName, string utilityId, double utilityPrice)
    {
        UtilityName = utilityName;
        UtilityId = utilityId;
        UtilityPrice = utilityPrice;
    }

    public string UtilityName { get; }
    public string UtilityId { get; }
    public double UtilityPrice { get; }
    public IReadOnlyList<RoomGroup> FreeForGroups => _freeForGroups.AsReadOnly();

    public bool IsFreeForGroup(RoomGroup group) => _freeForGroups.Contains(group);

    public void AddFreeGroup(RoomGroup group)
    {
        ArgumentNullException.ThrowIfNull(group);
        if (!_freeForGroups.Contains(group))
            _freeForGroups.Add(group);
    }

    public override string ToString() =>
        $"Utility{{UtilityName='{UtilityName}', UtilityId='{UtilityId}', UtilityPrice={UtilityPrice}}}";
}
