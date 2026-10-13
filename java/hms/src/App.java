import java.time.LocalDateTime;
import java.util.List;

import model.*;
import model.enums.*;
import service.hotelService;

public class App {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("[PASS] " + name);
        } else {
            failed++;
            System.out.println("[FAIL] " + name);
        }
    }

    private static void checkDouble(
            String name, double actual, double expected) {
        check(name, Math.abs(actual - expected) < 0.001);
        if (Math.abs(actual - expected) >= 0.001) {
            System.out.println("       Expected: " + expected
                    + ", actual: " + actual);
        }
    }

    public static void main(String[] args) {

        hotelService hotel = new hotelService();

        LocalDateTime start =
                LocalDateTime.of(2026, 10, 5, 14, 0);
        LocalDateTime end =
                LocalDateTime.of(2026, 10, 8, 12, 0);
        LocalDateTime checkIn =
                LocalDateTime.of(2026, 10, 5, 15, 0);
        LocalDateTime checkOut =
                LocalDateTime.of(2026, 10, 9, 12, 0);

        System.out.println("\n===== SETUP =====");

        utility wifi = new utility("WiFi", "U01", 10.0);
        utility breakfast = new utility("Breakfast", "U02", 20.0);

        roomGroup deluxe = new roomGroup(
                "Deluxe Room", "RG01", roomType.DELUXE);

        deluxe.addUtility(wifi);
        deluxe.addUtility(breakfast);

        // WiFi is free for Deluxe; breakfast remains chargeable.
        wifi.addFreeGroup(deluxe);

        Room room = new Room(
                "1st Floor", "R101", 2, deluxe,
                roomStatus.AVAILABLE, 100.0);

        Customer customer = new Customer(
                "KH01", "Alden", "0900000000",
                "012345678901", customerType.REGULAR);

        check("Add customer", hotel.addCustomer(customer));
        check("Add room", hotel.addRoom(room));
        check("Find customer", hotel.findCustomer("KH01") == customer);
        check("Find room", hotel.findRoom("R101") == room);
        check("Unknown customer returns null",
                hotel.findCustomer("UNKNOWN") == null);
        check("Unknown room returns null",
                hotel.findRoom("UNKNOWN") == null);

        System.out.println("\n===== BOOKING TESTS =====");

        check("Reject unknown room",
                !hotel.bookRoom("UNKNOWN", "KH01", start, end));

        check("Reject unknown customer",
                !hotel.bookRoom("R101", "UNKNOWN", start, end));

        check("Reject null start time",
                !hotel.bookRoom("R101", "KH01", null, end));

        check("Reject null end time",
                !hotel.bookRoom("R101", "KH01", start, null));

        check("Reject equal start/end",
                !hotel.bookRoom("R101", "KH01", start, start));

        check("Reject end before start",
                !hotel.bookRoom("R101", "KH01", end, start));

        check("Book room successfully",
                hotel.bookRoom("R101", "KH01", start, end));

        // The first successful booking in a fresh run receives C0.
        RentalContract contract = hotel.findContractById("C0");

        check("Find created contract", contract != null);
        check("Room becomes BOOKED",
                room.getRoomStatus() == roomStatus.BOOKED);
        check("Reject duplicate booking",
                !hotel.bookRoom("R101", "KH01", start, end));
        check("Unknown contract returns null",
                hotel.findContractById("UNKNOWN") == null);

        System.out.println("\n===== CHECK-IN TESTS =====");

        check("Reject check-in for unknown contract",
                !hotel.checkIn("UNKNOWN", checkIn));

        check("Reject null check-in time",
                !hotel.checkIn("C0", null));

        check("Reject check-in before booking starts",
                !hotel.checkIn("C0",
                        LocalDateTime.of(2026, 10, 5, 13, 0)));

        check("Reject checkout before check-in",
                !hotel.checkOut("C0", checkIn));

        check("Reject utility before check-in",
                !hotel.useUtility("C0", wifi));

        check("Check in successfully",
                hotel.checkIn("C0", checkIn));

        check("Check-in time recorded",
                contract.getCheckInTime().equals(checkIn));

        check("Reject duplicate check-in",
                !hotel.checkIn("C0", checkIn));

        check("Reject check-in after end time",
                !hotel.checkIn("C0",
                        LocalDateTime.of(2026, 10, 9, 10, 0)));

        System.out.println("\n===== UTILITY TESTS =====");

        check("Use WiFi during stay",
                hotel.useUtility("C0", wifi));

        check("Use breakfast during stay",
                hotel.useUtility("C0", breakfast));

        check("Reject null utility",
                !hotel.useUtility("C0", null));

        check("Reject utility for unknown contract",
                !hotel.useUtility("UNKNOWN", wifi));

        check("Both utilities recorded",
                contract.getUtilitiesUsed().size() == 2);

        // Verify the returned list cannot modify the contract's list.
        boolean listProtected = false;
        try {
            contract.getUtilitiesUsed().clear();
        } catch (UnsupportedOperationException e) {
            listProtected = true;
        }

        check("Utilities list is unmodifiable",
                listProtected
                && contract.getUtilitiesUsed().size() == 2);

        System.out.println("\n===== CHECK-OUT TESTS =====");

        check("Reject null checkout time",
                !hotel.checkOut("C0", null));

        check("Reject checkout before check-in time",
                !hotel.checkOut("C0",
                        LocalDateTime.of(2026, 10, 5, 14, 0)));

        check("Check out successfully",
                hotel.checkOut("C0", checkOut));

        check("Contract becomes completed",
                contract.getStatus() == contractStatus.completed);

        check("Checkout time recorded",
                contract.getCheckOutTime().equals(checkOut));

        check("Room becomes AVAILABLE",
                room.getRoomStatus() == roomStatus.AVAILABLE);

        check("Reject duplicate checkout",
                !hotel.checkOut("C0", checkOut));

        check("Reject utility after checkout",
                !hotel.useUtility("C0", wifi));

        System.out.println("\n===== FEE TESTS =====");

        // Oct 5 -> Oct 9 = 4 calendar days.
        checkDouble("Room fee", hotel.calculateRoomFee(contract), 400.0);

        // Deluxe overstay: 100 * 1.5 * 10 = 1,500 per day.
        checkDouble("Overstay fee",
                hotel.calculateOverstayFee(contract), 1500.0);

        // WiFi is free; breakfast costs 20.
        checkDouble("Utility fee",
                hotel.calculateUtilityFee(contract), 20.0);

        System.out.println("\n===== INVOICE TESTS =====");

        System.out.println("Expected invoice total: 1880.0");
        // 400 room + 1500 overstay + 20 utilities - 40 deposit.
        hotel.createInvoice(contract, customer, paymentType.CASH);

        // Should print a duplicate-invoice rejection, not another invoice.
        hotel.createInvoice(contract, customer, paymentType.CARD);

        System.out.println("\n===== FINAL STATE =====");
        System.out.println(contract);
        System.out.println(room);

        System.out.println("\n===== TEST SUMMARY =====");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Total:  " + (passed + failed));

        if (failed > 0) {
            System.out.println("RESULT: SOME TESTS FAILED");
        } else {
            System.out.println("RESULT: ALL ASSERTIONS PASSED");
        }
    }
}

/*
Expected fees: room = 400, overstay = 1500, utilities = 20, invoice total = 1880.

Invoice test limitation: createInvoice() returns void and the current service exposes no invoice lookup/count method. 
So this runner checks the console output manually for the duplicate-invoice rejection rather than asserting the invoice count.

Potential business-rule issue: calculateRoomFee() charges by calendar-date difference, so a same-day stay currently costs 0. 
This runner doesn't hide that behavior behind a passing assertion; decide whether same-day stays should incur a full day's charge and add a dedicated test for your chosen policy.

C0 assumes this is a fresh application run with no earlier successful booking in that JVM. 
That is true for the command above


how to Run:

cd java/hms
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out App

*/