abstract room
        +presidentRoom
        +normalRoom
        +deluxeRoom
abstract roomGroup
        +
        +

abstract customer


HotelManagementSystem/
└── src/
    ├── model/
    │   ├── Customer.java
    │   ├── CustomerGroup.java
    │   ├── Room.java
    │   ├── RoomGroup.java
    │   ├── RentalContract.java
    │   ├── Invoice.java
    │   └── Utility.java
    │
    ├── service/
    │   ├── CustomerService.java
    │   ├── RoomService.java
    │   ├── BookingService.java
    │   └── InvoiceService.java
    │
    ├── repository/
    │   └── ...
    │
    └── Main.java
    UB100KF69CD6F


1. mối quan hệ có thể sử dụng util chưa rõ ragn2 lắm
2. useUtility() chưa rõ ràng. Hợp đồng end vẫn có thể use?