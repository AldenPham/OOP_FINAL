package hotel.dao;

import hotel.data.HotelRepository;
import java.sql.*;

public final class RoomTypeRevenueDAO extends ReportDAO {
    public RoomTypeRevenueDAO(HotelRepository repository) { super(repository); }

    public void execute() {
        String sql = """
            SELECT rg.Type AS RoomType, COALESCE(SUM(i.Total), 0) AS TotalRevenue
            FROM RoomGroups rg
            LEFT JOIN Rooms r ON r.RoomGroupId = rg.GroupId
            LEFT JOIN RentalContracts rc ON rc.RoomId = r.RoomId
            LEFT JOIN Invoices i ON i.ContractId = rc.ContractId
            GROUP BY rg.Type ORDER BY TotalRevenue DESC, RoomType
            """;
        try (Connection c = open(); PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {
            System.out.println("=== DOANH THU THEO HẠNG PHÒNG ===");
            while (rs.next())
                System.out.printf("Hạng phòng: %-12s | Tổng doanh thu: %,.0f VNĐ%n",
                    rs.getString("RoomType"), rs.getDouble("TotalRevenue"));
        } catch (SQLException e) { System.err.println("Lỗi truy vấn doanh thu theo hạng phòng: " + e.getMessage()); }
    }
}
