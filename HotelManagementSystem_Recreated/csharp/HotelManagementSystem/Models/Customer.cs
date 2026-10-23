using HotelManagementSystem.Models.Enums;

namespace HotelManagementSystem.Models;

public sealed class Customer(string id, string name, string phoneNumber, string cccd, CustomerType type)
    : Person(id, name, phoneNumber)
{
    public string Cccd { get; } = cccd ?? string.Empty;
    public CustomerType Type { get; } = type;
    public int TimeStayed { get; internal set; }
    public int MoneySpended { get; internal set; }
    public override string GetRole() => "Customer";
    public override string ToString() => $"{Id} | {Name} | phone: {PhoneNumber} | type: {Type}";
}
