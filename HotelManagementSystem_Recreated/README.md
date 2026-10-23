# Hotel Management System — C# and Java recreations

This archive contains two independent implementations recreated from `AldenPham/OOP_FINAL`. They preserve the original console workflows and business rules while separating models, services, and SQLite persistence.

## Included

- `csharp/HotelManagementSystem/` — .NET 8 console app using `Microsoft.Data.Sqlite`.
- `java/HotelManagementSystem/` — Java 17+ console app using Maven and Xerial SQLite JDBC.

Both versions include customers, room groups, utilities and free/paid group assignments, rooms, rental contracts, check-in/check-out, utility usage, billing, invoices, and the same ten analytics reports.

## Architecture

`App` handles console input/output. `HotelService` owns business rules and the in-memory object graph. `HotelRepository` owns SQLite reads/writes. On startup, the repository creates missing tables if necessary and loads records in dependency order, restoring references between customers, groups, utilities, rooms, contracts, and invoices.

All accepted state changes are persisted. Room booking inserts a contract and marks the room `BOOKED` in a transaction. Check-out marks the contract completed and releases the room in a transaction. Repeated usage of one utility is stored as separate usage events, as in the original business logic. Primary/foreign keys protect referential integrity; an invoice is unique per contract.

## ID convention

IDs remain human-readable and use the original prefixes. To follow the requested “start at 0” rule, an empty database starts with `KH0`, `RG0`, `UT0`, `R0`, `C0`, and `I0`. The next ID is derived from the highest existing numeric suffix in the loaded data, so IDs continue after an application restart. SQLite primary-key constraints are the final duplicate guard.

## Run the C# app

Requires the .NET 8 SDK and NuGet access on first restore.

```bash
cd csharp/HotelManagementSystem
dotnet restore
dotnet run
```

Run the C# integration smoke test against a temporary database:

```bash
dotnet run -- --self-test
```

The app creates `hotel.db` in the current working directory by default. You can back up or remove this file to reset the local data (stop the app before doing so).

## Run the Java app

Requires Java 17+ and Maven. Maven downloads the SQLite JDBC driver on first build.

```bash
cd java/HotelManagementSystem
mvn clean package
mvn exec:java
```

Run the Java integration smoke test against a temporary database:

```bash
mvn test-compile exec:java -Dexec.mainClass=hotel.PersistenceSmoke -Dexec.classpathScope=test
```

Pass an alternate database path as the first application argument if running the main class directly. The default is `hotel.db` in the current working directory.

## Preserved business rules

- Booking requires an existing customer, an existing available room, and a start time before the end time.
- The source project did not enforce overlapping booking dates, so this recreation does not silently add that behavior.
- Check-in must be within the booked interval, must occur once, and must include a positive number of people.
- Utility use requires an ongoing contract that has checked in and not checked out. Each usage is charged separately unless the utility is free for the room's group.
- Check-out requires check-in and cannot precede check-in. The room becomes available when the stay is completed.
- Room fee is based on calendar-day difference from booking start to actual check-out; same-day check-out can therefore yield zero room-fee days, matching the source README.
- Overstay fees use calendar-day difference after the booked end. Over-capacity fees apply for each person above room capacity.
- The deposit is 10% of room fees and is deducted from the total. An invoice may be created once the contract is checked out, and only one invoice per contract is accepted.
- Quarterly revenue is grouped by checkout date because the original model has no separate payment timestamp.

## Verification notes

The Java source was compiled in this environment with `javac --release 17`. This environment did not have the .NET SDK or Maven installed and could not reach GitHub/NuGet/Maven from the shell, so C# compilation and live SQLite-JDBC runtime tests could not be executed here. Both projects are packaged with build configuration and dependency declarations; run the commands above to perform full local restore/build/runtime verification.
