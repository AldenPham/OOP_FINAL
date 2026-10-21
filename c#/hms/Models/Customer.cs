using OOP_FINAL_CSharp.Models.Enums;

namespace OOP_FINAL_CSharp.Models;

public sealed class Customer : Person
{
    public Customer(
        string id,
        string name,
        string phoneNumber,
        string cccd,
        CustomerType type) : base(id, name, phoneNumber)
    {
        Cccd = cccd;
        Type = type;
    }

    public string Cccd { get; }
    public int TimeStayed { get; internal set; }
    public int MoneySpended { get; internal set; }
    public CustomerType Type { get; }

    public string GetCustomerName() => Name;
    public override string GetRole() => "Customer";

    public override string ToString() =>
        $"Customer{{Id='{Id}', Name='{Name}', PhoneNumber='{PhoneNumber}', " +
        $"Cccd='{Cccd}', TimeStayed={TimeStayed}, MoneySpended={MoneySpended}, Type={Type}}}";
}
