package hotel.dao;

import hotel.data.HotelRepository;
import java.sql.*;

public final class RoomGroupUtilityDAO extends ReportDAO {
    public RoomGroupUtilityDAO(HotelRepository repository) { super(repository); }

    public void execute(String roomGroupId) {
        String sql = """
            SELECT rg.GroupName, u.UtilityId, u.UtilityName, u.UtilityPrice, rgu.IsFree
            FROM RoomGroupUtilities rgu
            JOIN RoomGroups rg ON rg.GroupId = rgu.RoomGroupId
            JOIN Utilities u ON u.UtilityId = rgu.UtilityId
            WHERE rg.GroupId = ? ORDER BY u.UtilityName
            """;
        try (Connection c = open(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, roomGroupId);
            try (ResultSet rs = p.executeQuery()) {
                System.out.println("=== TIỆN ÍCH CỦA NHÓM PHÒNG: " + roomGroupId + " ===");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("- %s | %s | %,.0f VNĐ | %s%n",
                        rs.getString("UtilityId"), rs.getString("UtilityName"),
                        rs.getDouble("UtilityPrice"), rs.getInt("IsFree") != 0 ? "miễn phí" : "có phí");
                }
                if (!found) System.out.println("Không có tiện ích được gán cho nhóm phòng này.");
            }
        } catch (SQLException e) { System.err.println("Lỗi truy vấn tiện ích nhóm phòng: " + e.getMessage()); }
    }
}
