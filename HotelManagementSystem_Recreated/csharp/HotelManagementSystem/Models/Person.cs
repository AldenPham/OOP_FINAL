namespace HotelManagementSystem.Models;

public abstract class Person(string id, string name, string phoneNumber)
{
    public string Id { get; } = Require(id, nameof(id));
    public string Name { get; } = Require(name, nameof(name));
    public string PhoneNumber { get; } = phoneNumber ?? string.Empty;
    public abstract string GetRole();

    protected static string Require(string value, string name) =>
        string.IsNullOrWhiteSpace(value) ? throw new ArgumentException("Value cannot be blank.", name) : value.Trim();
}
