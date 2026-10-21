namespace OOP_FINAL_CSharp.Models;

public abstract class Person
{
    protected Person(string id, string name, string phoneNumber)
    {
        Id = id;
        Name = name;
        PhoneNumber = phoneNumber;
    }

    public string Id { get; }
    public string Name { get; }
    public string PhoneNumber { get; }
    public abstract string GetRole();

    public override string ToString() =>
        $"{GetRole()}{{Id='{Id}', Name='{Name}', PhoneNumber='{PhoneNumber}'}}";
}
