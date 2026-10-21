using OOP_FINAL_CSharp.Models;
using OOP_FINAL_CSharp.Models.Enums;
using OOP_FINAL_CSharp.Models.Policy;
using OOP_FINAL_CSharp.Services;

var hotel = new HotelService();

var deluxe = new RoomGroup("Deluxe Room", "RG01", RoomType.DELUXE, new FeePolicy(150, 80));
var standard = new RoomGroup("Standard Room", "RG02", RoomType.STANDARD, new FeePolicy(90, 50));

var wifi = new Utility("WiFi", "U01", 10);
var breakfast = new Utility("Breakfast", "U02", 20);
deluxe.AddUtility(wifi);
deluxe.AddUtility(breakfast);
wifi.AddFreeGroup(deluxe);

var room1 = new Room("1st Floor", "R101", 2, deluxe, RoomStatus.AVAILABLE, 100);
var room2 = new Room("2nd Floor", "R202", 1, standard, RoomStatus.AVAILABLE, 50);
var room3 = new Room("3rd Floor", "R303", 3, deluxe, RoomStatus.AVAILABLE, 150);
hotel.AddRoom(room1);
hotel.AddRoom(room2);
hotel.AddRoom(room3);

var customer1 = new Customer("KH01", "Alden", "0900000000", "012345678901", CustomerType.REGULAR);
var customer2 = new Customer("KH02", "Guest Two", "0900000001", "012345678902", CustomerType.VIP);
hotel.AddCustomer(customer1);
hotel.AddCustomer(customer2);

var start = new DateTime(2026, 1, 10, 14, 0, 0);
var end = new DateTime(2026, 1, 13, 12, 0, 0);
var checkIn = new DateTime(2026, 1, 10, 15, 0, 0);
var checkOut = new DateTime(2026, 1, 15, 12, 0, 0);

Console.WriteLine("=== DEMO: HOTEL BOOKING (C#) ===");
hotel.BookRoom("R101", "KH01", start, end);
var contract = hotel.FindContractById("C0")!;
hotel.CheckIn("C0", checkIn, 3);
hotel.UseUtility("C0", wifi);
hotel.UseUtility("C0", breakfast);
hotel.CheckOut("C0", checkOut);
hotel.CreateInvoice(contract, customer1, PaymentType.CASH);

// One completed stay for the second customer demonstrates a separate group too.
hotel.BookRoom("R202", "KH02", start, end);
var contract2 = hotel.FindContractById("C1")!;
hotel.CheckIn("C1", checkIn, 1);
hotel.CheckOut("C1", new DateTime(2026, 1, 13, 12, 0, 0));
hotel.CreateInvoice(contract2, customer2, PaymentType.CARD);


