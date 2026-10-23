package hotel.model.policy;

public record FeePolicy(double overstayFeePerDay, double overMaxCapacityFee) {
    public FeePolicy {
        if (!Double.isFinite(overstayFeePerDay) || overstayFeePerDay < 0 ||
            !Double.isFinite(overMaxCapacityFee) || overMaxCapacityFee < 0) {
            throw new IllegalArgumentException("Fee values must be finite and non-negative.");
        }
    }
}
