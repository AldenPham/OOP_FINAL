package hotel.dao;

import hotel.data.HotelRepository;
import java.sql.*;

public final class QuarterlyRevenueDAO extends ReportDAO {
    public QuarterlyRevenueDAO(HotelRepository repository) { super(repository); }

    public void execute() {
        // Schema has no invoice issue timestamp; report by contract check-out time instead.
        String sql = """
            SELECT strftime('%Y', rc.CheckOutTime) AS RevenueYear,
                   (CAST(strftime('%m', rc.CheckOutTime) AS INTEGER) + 2) / 3 AS RevenueQuarter,
                   SUM(i.Total) AS TotalRevenue
            FROM Invoices i JOIN RentalContracts rc ON rc.ContractId = i.ContractId
            WHERE rc.CheckOutTime IS NOT NULL
            GROUP BY RevenueYear, RevenueQuarter
            ORDER BY RevenueYear DESC, RevenueQuarter DESC
            """;
        try (Connection c = open(); PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {
            System.out.println("=== BÁO CÁO DOANH THU THEO QUÝ (NGÀY CHECK-OUT) ===");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("Năm %s - Quý %d | Doanh thu: %,.0f VNĐ%n",
                    rs.getString("RevenueYear"), rs.getInt("RevenueQuarter"), rs.getDouble("TotalRevenue"));
            }
            if (!found) System.out.println("Chưa có hóa đơn có ngày check-out.");
        } catch (SQLException e) { System.err.println("Lỗi truy vấn doanh thu theo quý: " + e.getMessage()); }
    }
}
