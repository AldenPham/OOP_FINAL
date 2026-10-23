# C# Hotel Management System

.NET 8 console app, with model classes, service-level business rules, and a SQLite repository.

```bash
dotnet restore
dotnet run
```

Run a temporary-database integration smoke test (checks persistence across restart, repeated utility use, billing, state restoration, and ID continuation):

```bash
dotnet run -- --self-test
```

`hotel.db` is created in the current working directory. `HotelRepository` creates missing tables and loads the existing database on startup. Records are inserted/updated through the repository when accepted operations occur. SQLite is provided by `Microsoft.Data.Sqlite`.

The 18-option menu preserves the original workflows and includes all ten reports also present in the Java source. Database IDs are text keys with the original prefixes; the first suffix for an empty table is 0.
