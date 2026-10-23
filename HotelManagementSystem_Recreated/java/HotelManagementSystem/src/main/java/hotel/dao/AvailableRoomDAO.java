package hotel.dao;

import hotel.data.HotelRepository;
import java.sql.*;

public final class AvailableRoomDAO extends ReportDAO {
    public AvailableRoomDAO(HotelRepository repository) { super(repository); }

    public void execute() {
        String sql = """
            SELECT r.RoomId, r.RoomLocation, rg.Type AS RoomType, r.RoomPrice
            FROM Rooms r JOIN RoomGroups rg ON rg.GroupId = r.RoomGroupId
            WHERE r.Status = ? ORDER BY r.RoomId
            """;
        try (Connection c = open(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, "AVAILABLE");
            try (ResultSet rs = p.executeQuery()) {
                System.out.println("=== DANH SÁCH PHÒNG ĐANG TRỐNG ===");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("Mã phòng: %s | Vị trí: %s | Loại: %s | Giá: %,.0f%n",
                        rs.getString("RoomId"), rs.getString("RoomLocation"),
                        rs.getString("RoomType"), rs.getDouble("RoomPrice"));
                }
                if (!found) System.out.println("Không có phòng trống.");
            }
        } catch (SQLException e) { System.err.println("Lỗi truy vấn phòng trống: " + e.getMessage()); }
    }
}
