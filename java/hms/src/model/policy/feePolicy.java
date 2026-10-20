package model.policy;

public record feePolicy(
    double roomFee,
    double overStayFeePerDay,
    double overMaxCapacityFee
) {
    public double getRoomFeePerDay() {
        return roomFee;
    }

    public double getOverStayFeePerDay() {
        return overStayFeePerDay;
    }

    public double getOverMaxCapacityFee() {
        return overMaxCapacityFee;
    }
}


