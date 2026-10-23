using HotelManagementSystem.Models.Enums;

namespace HotelManagementSystem.Models;

public sealed class Invoice(string invoiceId, Customer payer, RentalContract contract, PaymentType paymentType,
    double roomFee, double overstayFee, double overMaxCapacityFee, double utilityFee, double deposit, double total)
{
    public string InvoiceId { get; } = string.IsNullOrWhiteSpace(invoiceId) ? throw new ArgumentException("Invoice ID cannot be blank.", nameof(invoiceId)) : invoiceId.Trim();
    public Customer Payer { get; } = payer ?? throw new ArgumentNullException(nameof(payer));
    public RentalContract Contract { get; } = contract ?? throw new ArgumentNullException(nameof(contract));
    public PaymentType PaymentType { get; } = paymentType;
    public double RoomFee { get; } = roomFee;
    public double OverstayFee { get; } = overstayFee;
    public double OverMaxCapacityFee { get; } = overMaxCapacityFee;
    public double UtilityFee { get; } = utilityFee;
    public double Deposit { get; } = deposit;
    public double Total { get; } = total;
    public override string ToString() => $"Invoice {InvoiceId} | contract: {Contract.ContractId} | payer: {Payer.Name} | payment: {PaymentType} | total: {Total:F2}";
}
