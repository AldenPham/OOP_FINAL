using OOP_FINAL_CSharp.Models.Enums;

namespace OOP_FINAL_CSharp.Models;

public sealed class Invoice
{
    public Invoice(
        string invoiceId,
        Customer payer,
        RentalContract contract,
        PaymentType paymentType,
        double roomFee,
        double overstayFee,
        double overMaxCapacityFee,
        double utilityFee,
        double deposit,
        double total)
    {
        InvoiceId = invoiceId;
        Payer = payer ?? throw new ArgumentNullException(nameof(payer));
        Contract = contract ?? throw new ArgumentNullException(nameof(contract));
        PaymentType = paymentType;
        RoomFee = roomFee;
        OverstayFee = overstayFee;
        OverMaxCapacityFee = overMaxCapacityFee;
        UtilityFee = utilityFee;
        Deposit = deposit;
        Total = total;
    }

    public string InvoiceId { get; }
    public Customer Payer { get; }
    public RentalContract Contract { get; }
    public PaymentType PaymentType { get; }
    public double RoomFee { get; }
    public double OverstayFee { get; }
    public double UtilityFee { get; }
    public double OverMaxCapacityFee { get; }
    public double Deposit { get; }
    public double Total { get; }

    public override string ToString() =>
        $"Invoice{{InvoiceId='{InvoiceId}', Payer={Payer.GetCustomerName()}, " +
        $"Contract={Contract.ContractId}, PaymentType={PaymentType}, RoomFee={RoomFee}, " +
        $"OverstayFee={OverstayFee}, UtilityFee={UtilityFee}, OverMaxCapacityFee={OverMaxCapacityFee}, " +
        $"Deposit={Deposit}, Total={Total}}}";
}
