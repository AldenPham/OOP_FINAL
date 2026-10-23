package hotel.dao;

import hotel.data.HotelRepository;
import java.sql.*;

public final class CustomerReturnFrequencyDAO extends ReportDAO {
    public CustomerReturnFrequencyDAO(HotelRepository repository) { super(repository); }

    public void execute() {
        String sql = """
            SELECT c.Id, c.Name, COUNT(rc.ContractId) AS VisitCount
            FROM Customers c JOIN RentalContracts rc ON rc.CustomerId = c.Id
            WHERE rc.Status = ?
            GROUP BY c.Id, c.Name HAVING COUNT(rc.ContractId) > 1
            ORDER BY VisitCount DESC, c.Name
            """;
        try (Connection c = open(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, "COMPLETED");
            try (ResultSet rs = p.executeQuery()) {
                System.out.println("=== THỐNG KÊ SỐ LẦN KHÁCH QUAY TRỞ LẠI ===");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    int visits = rs.getInt("VisitCount");
                    System.out.printf("Khách: %s (%s) | Lưu trú hoàn tất: %d | Lượt quay lại: %d%n",
                        rs.getString("Name"), rs.getString("Id"), visits, visits - 1);
                }
                if (!found) System.out.println("Chưa có khách quay lại lần thứ hai.");
            }
        } catch (SQLException e) { System.err.println("Lỗi truy vấn tần suất khách quay lại: " + e.getMessage()); }
    }
}
