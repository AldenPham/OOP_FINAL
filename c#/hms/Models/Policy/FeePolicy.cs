namespace OOP_FINAL_CSharp.Models.Policy;

public sealed record FeePolicy(
    double OverStayFeePerDay,
    double OverMaxCapacityFee
);
