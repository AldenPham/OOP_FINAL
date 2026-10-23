package hotel.dao;

import hotel.data.HotelRepository;
import java.sql.*;

public final class TopLoyalCustomerDAO extends ReportDAO {
    public TopLoyalCustomerDAO(HotelRepository repository) { super(repository); }

    public void execute() {
        String sql = """
            WITH VisitCounts AS (
                SELECT c.Id, c.Name, COUNT(rc.ContractId) AS VisitCount
                FROM Customers c JOIN RentalContracts rc ON rc.CustomerId = c.Id
                WHERE rc.Status = ?
                GROUP BY c.Id, c.Name
            )
            SELECT Id, Name, VisitCount FROM VisitCounts
            WHERE VisitCount = (SELECT MAX(VisitCount) FROM VisitCounts)
            ORDER BY Name
            """;
        try (Connection c = open(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, "COMPLETED");
            try (ResultSet rs = p.executeQuery()) {
                System.out.println("=== KHÁCH HÀNG TRUNG THÀNH NHẤT ===");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    int visits = rs.getInt("VisitCount");
                    System.out.printf("Khách hàng: %s (%s) | Lưu trú hoàn tất: %d | Lượt quay lại: %d%n",
                        rs.getString("Name"), rs.getString("Id"), visits, Math.max(0, visits - 1));
                }
                if (!found) System.out.println("Chưa có khách hàng nào hoàn tất lưu trú.");
            }
        } catch (SQLException e) { System.err.println("Lỗi truy vấn khách hàng trung thành: " + e.getMessage()); }
    }
}
