package hotel.dao;

import hotel.data.HotelRepository;
import java.sql.*;

public final class CustomerTotalSpendDAO extends ReportDAO {
    public CustomerTotalSpendDAO(HotelRepository repository) { super(repository); }

    public void execute() {
        String sql = """
            SELECT c.Id, c.Name, COALESCE(SUM(i.Total), 0) AS TotalSpent
            FROM Customers c LEFT JOIN Invoices i ON i.PayerId = c.Id
            GROUP BY c.Id, c.Name ORDER BY TotalSpent DESC, c.Name
            """;
        try (Connection c = open(); PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {
            System.out.println("=== TỔNG CHI TIÊU THEO NGƯỜI THANH TOÁN ===");
            while (rs.next())
                System.out.printf("Khách hàng: %-22s (%s) | Tổng chi: %,.0f VNĐ%n",
                    rs.getString("Name"), rs.getString("Id"), rs.getDouble("TotalSpent"));
        } catch (SQLException e) { System.err.println("Lỗi truy vấn tổng chi tiêu: " + e.getMessage()); }
    }
}
