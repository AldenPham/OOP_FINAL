package hotel.dao;

import hotel.data.HotelRepository;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Shared connection helper for reports; uses the same database file as HotelRepository. */
abstract class ReportDAO {
    protected final HotelRepository repository;

    protected ReportDAO(HotelRepository repository) {
        this.repository = repository;
    }

    protected Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(
            "jdbc:sqlite:" + repository.getDatabasePath().toAbsolutePath().normalize());
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }
}
