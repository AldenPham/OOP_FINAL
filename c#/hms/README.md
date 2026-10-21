# OOP_FINAL — C# version

Đây là project C# độc lập được chuyển từ cấu trúc Java trong `java/hms/src` của repo:
https://github.com/AldenPham/OOP_FINAL

## Requirements
- .NET 8 SDK (or newer compatible SDK).

## Run
From this folder:

```bash
dotnet run
```

## Structure

```text
OOP_FINAL_CSharp/
├── OOP_FINAL_CSharp.csproj
├── Program.cs
├── Models/
│   ├── Person.cs
│   ├── Customer.cs
│   ├── CustomerGroup.cs
│   ├── Room.cs
│   ├── RoomGroup.cs
│   ├── Utility.cs
│   ├── RentalContract.cs
│   ├── Invoice.cs
│   ├── Enums/
│   └── Policy/
├── Services/
│   └── HotelService.cs
└── Analytics/
    └── HotelAnalytics.cs
```

## Porting notes
- Java classes became C# classes with PascalCase names and properties.
- `feePolicy` record became `FeePolicy` record.
- Java `List.copyOf`/private lists are represented with read-only list views where practical.
- `hotelService` is ported as `HotelService`; the booking/check-in/utility/check-out and billing flow is retained.
- LINQ expressions and lambdas power the analytics reports.
- Fee calculations preserve the Java implementation's calendar-date behavior: same-day checkout can produce zero room-fee days.
- Quarterly revenue uses contract checkout date because the current Java model does not store a separate invoice/payment timestamp.
- This project keeps the demonstrated Java rules. It does not add room-date-overlap checking because the Java service does not implement it.
