package hotel.dao;

import hotel.data.HotelRepository;
import java.sql.*;

public final class MostRentedRoomTypeDAO extends ReportDAO {
    public MostRentedRoomTypeDAO(HotelRepository repository) { super(repository); }

    public void execute() {
        String sql = """
            SELECT rg.Type AS RoomType, COUNT(rc.ContractId) AS TotalRentals
            FROM RoomGroups rg JOIN Rooms r ON r.RoomGroupId = rg.GroupId
            JOIN RentalContracts rc ON rc.RoomId = r.RoomId
            WHERE rc.Status <> ?
            GROUP BY rg.Type ORDER BY TotalRentals DESC, RoomType
            """;
        try (Connection c = open(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, "CANCELLED");
            try (ResultSet rs = p.executeQuery()) {
                System.out.println("=== HẠNG PHÒNG ĐƯỢC THUÊ NHIỀU NHẤT ===");
                boolean found = false;
                int max = -1;
                while (rs.next()) {
                    int count = rs.getInt("TotalRentals");
                    if (!found) { found = true; max = count; }
                    if (count < max) break;
                    System.out.printf("Hạng phòng: %-12s | Số lượt thuê: %d%n",
                        rs.getString("RoomType"), count);
                }
                if (!found) System.out.println("Chưa có lượt thuê.");
            }
        } catch (SQLException e) { System.err.println("Lỗi truy vấn hạng phòng thuê nhiều nhất: " + e.getMessage()); }
    }
}
