namespace HotelManagementSystem.Models;

public sealed class Utility(string utilityName, string utilityId, double utilityPrice)
{
    private readonly HashSet<string> _freeForGroupIds = new(StringComparer.Ordinal);
    public string UtilityName { get; } = string.IsNullOrWhiteSpace(utilityName) ? throw new ArgumentException("Utility name cannot be blank.", nameof(utilityName)) : utilityName.Trim();
    public string UtilityId { get; } = string.IsNullOrWhiteSpace(utilityId) ? throw new ArgumentException("Utility ID cannot be blank.", nameof(utilityId)) : utilityId.Trim();
    public double UtilityPrice { get; } = utilityPrice >= 0 && double.IsFinite(utilityPrice) ? utilityPrice : throw new ArgumentOutOfRangeException(nameof(utilityPrice));
    public IReadOnlySet<string> FreeForGroupIds => _freeForGroupIds;
    public bool IsFreeForGroup(RoomGroup group) => _freeForGroupIds.Contains(group.GroupId);
    public void AddFreeGroup(RoomGroup group) { ArgumentNullException.ThrowIfNull(group); _freeForGroupIds.Add(group.GroupId); }
    public void RemoveFreeGroup(RoomGroup group) { ArgumentNullException.ThrowIfNull(group); _freeForGroupIds.Remove(group.GroupId); }
    public override string ToString() => $"{UtilityId} | {UtilityName} | {UtilityPrice:F2}";
}
