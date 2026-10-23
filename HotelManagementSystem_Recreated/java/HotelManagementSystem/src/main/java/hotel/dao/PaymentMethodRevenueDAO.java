package hotel.dao;

import hotel.data.HotelRepository;
import java.sql.*;

public final class PaymentMethodRevenueDAO extends ReportDAO {
    public PaymentMethodRevenueDAO(HotelRepository repository) { super(repository); }

    public void execute() {
        String sql = """
            SELECT PaymentType, SUM(Total) AS TotalRevenue
            FROM Invoices GROUP BY PaymentType ORDER BY TotalRevenue DESC, PaymentType
            """;
        try (Connection c = open(); PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {
            System.out.println("=== DOANH THU THEO PHƯƠNG THỨC THANH TOÁN ===");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("Phương thức: %-15s | Tổng doanh thu: %,.0f VNĐ%n",
                    rs.getString("PaymentType"), rs.getDouble("TotalRevenue"));
            }
            if (!found) System.out.println("Chưa có hóa đơn.");
        } catch (SQLException e) { System.err.println("Lỗi truy vấn doanh thu theo phương thức: " + e.getMessage()); }
    }
}
