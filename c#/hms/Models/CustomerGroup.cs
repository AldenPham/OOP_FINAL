using OOP_FINAL_CSharp.Models.Enums;

namespace OOP_FINAL_CSharp.Models;

public sealed class CustomerGroup
{
    public CustomerGroup(string customerGroupId, CustomerType type)
    {
        CustomerGroupId = customerGroupId;
        Type = type;
    }

    public string CustomerGroupId { get; }
    public CustomerType Type { get; }

    public override string ToString() =>
        $"CustomerGroup{{CustomerGroupId='{CustomerGroupId}', Type={Type}}}";
}
