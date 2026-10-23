package hotel.dao;

import hotel.data.HotelRepository;
import java.sql.*;

public final class LeastBookedRoomTypeDAO extends ReportDAO {
    public LeastBookedRoomTypeDAO(HotelRepository repository) { super(repository); }

    public void execute() {
        String sql = """
            WITH BookingCounts AS (
                SELECT rg.Type AS RoomType, COUNT(rc.ContractId) AS TotalBookings
                FROM RoomGroups rg
                LEFT JOIN Rooms r ON r.RoomGroupId = rg.GroupId
                LEFT JOIN RentalContracts rc ON rc.RoomId = r.RoomId AND rc.Status <> ?
                GROUP BY rg.Type
            )
            SELECT RoomType, TotalBookings FROM BookingCounts
            WHERE TotalBookings = (SELECT MIN(TotalBookings) FROM BookingCounts)
            ORDER BY RoomType
            """;
        try (Connection c = open(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, "CANCELLED");
            try (ResultSet rs = p.executeQuery()) {
                System.out.println("=== HẠNG PHÒNG ĐƯỢC ĐẶT ÍT NHẤT ===");
                while (rs.next())
                    System.out.printf("Hạng phòng: %s | Số lượt đặt: %d%n",
                        rs.getString("RoomType"), rs.getInt("TotalBookings"));
            }
        } catch (SQLException e) { System.err.println("Lỗi truy vấn hạng phòng ít được đặt: " + e.getMessage()); }
    }
}
