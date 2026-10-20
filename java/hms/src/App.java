import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import model.Customer;
import model.RentalContract;
import model.Room;
import model.customerGroup;
import model.roomGroup;
import model.utility;
import model.enums.contractStatus;
import model.enums.customerType;
import model.enums.paymentType;
import model.enums.roomStatus;
import model.enums.roomType;
import model.policy.feePolicy;
import service.hotelService;

/**
 * Integration-style test runner for the current OOP_FINAL Java model/service API.
 * Run from java/hms after replacing src/App.java with this file:
 *   javac -d out $(find src -name '*.java') && java -cp out App
 *
 * This runner tests implemented behavior. It does not pretend to test unresolved
 * policies such as duplicate IDs or whether every utility must belong to a room group.
 */
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

    private static void checkDouble(String name, double actual, double expected) {
        check(name + " (expected=" + expected + ", actual=" + actual + ")",
                Math.abs(actual - expected) < 0.001);
    }

    private static String captureStdout(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            action.run();
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    public static void main(String[] args) {
        hotelService hotel = new hotelService();

        LocalDateTime start = LocalDateTime.of(2026, 10, 5, 14, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 8, 12, 0);
        LocalDateTime checkIn = LocalDateTime.of(2026, 10, 5, 15, 0);
        LocalDateTime checkOut = LocalDateTime.of(2026, 10, 10, 12, 0);

        System.out.println("===== 1. MODEL / RECORD / ENUM TESTS =====");
        feePolicy deluxePolicy = new feePolicy(150.0, 80.0);
        feePolicy standardPolicy = new feePolicy(90.0, 50.0);
        roomGroup deluxe = new roomGroup("Deluxe Room", "RG01", roomType.DELUXE, deluxePolicy);
        roomGroup standard = new roomGroup("Standard Room", "RG02", roomType.STANDARD, standardPolicy);

        utility wifi = new utility("WiFi", "U01", 10.0);
        utility breakfast = new utility("Breakfast", "U02", 20.0);
        deluxe.addUtility(wifi);
        deluxe.addUtility(breakfast);
        wifi.addFreeGroup(deluxe);

        check("feePolicy stores overstay fee", deluxe.getFee().overStayFeePerDay() == 150.0);
        check("feePolicy stores over-capacity fee", deluxe.getFee().overMaxCapacityFee() == 80.0);
        check("roomGroup exposes configured room type", deluxe.getType() == roomType.DELUXE);
        check("roomGroup stores added utilities", deluxe.getUtilities().size() == 2);
        check("utility can be free for configured group", wifi.isFreeForGroup(deluxe));
        check("utility is not automatically free for another group", !wifi.isFreeForGroup(standard));
        check("paid utility is not free by default", !breakfast.isFreeForGroup(deluxe));
        wifi.addFreeGroup(deluxe); // duplicate addition should leave membership true
        check("adding the same free group again remains safe", wifi.isFreeForGroup(deluxe));

        Room room = new Room("1st Floor", "R101", 2, deluxe, roomStatus.AVAILABLE, 100.0);
        Room room2 = new Room("2nd Floor", "R202", 1, standard, roomStatus.AVAILABLE, 50.0);
        Customer customer = new Customer("KH01", "Alden", "0900000000",
                "012345678901", customerType.REGULAR);
        Customer customer2 = new Customer("KH02", "Guest Two", "0900000001",
                "012345678902", customerType.REGULAR);
        customerGroup group = new customerGroup("CG01", customerType.REGULAR);

        check("Room constructor stores room price", room.getRoomPrice() == 100.0);
        check("Room exposes its ID", "R101".equals(room.getRoomId()));
        check("Room exposes its capacity", room.getRoomCapacity() == 2);
        check("Room exposes its group and type", room.getRoomGroup() == deluxe
                && room.getRoomType() == roomType.DELUXE && room.getInGroup() == deluxe);
        check("Room starts with provided status", room.getRoomStatus() == roomStatus.AVAILABLE);
        check("Customer inherits person ID/name/phone", "KH01".equals(customer.getId())
                && "Alden".equals(customer.getName())
                && "0900000000".equals(customer.getPhoneNumber()));
        check("Customer polymorphic role", "Customer".equals(customer.getRole()));
        check("customerGroup stores its ID", "CG01".equals(group.getCustomerGroupId()));

        System.out.println("\n===== 2. ADD / FIND TESTS =====");
        check("Add first customer", hotel.addCustomer(customer));
        check("Add second customer", hotel.addCustomer(customer2));
        check("Add first room", hotel.addRoom(room));
        check("Add second room", hotel.addRoom(room2));
        check("Find existing customer", hotel.findCustomer("KH01") == customer);
        check("Find existing room", hotel.findRoom("R101") == room);
        check("Unknown customer returns null", hotel.findCustomer("NO_SUCH_CUSTOMER") == null);
        check("Unknown room returns null", hotel.findRoom("NO_SUCH_ROOM") == null);
        check("Unknown contract returns null", hotel.findContractById("NO_SUCH_CONTRACT") == null);

        System.out.println("\n===== 3. BOOKING TESTS =====");
        check("Reject booking with unknown room", !hotel.bookRoom("NO_ROOM", "KH01", start, end));
        check("Reject booking with unknown customer", !hotel.bookRoom("R101", "NO_CUSTOMER", start, end));
        check("Reject null start time", !hotel.bookRoom("R101", "KH01", null, end));
        check("Reject null end time", !hotel.bookRoom("R101", "KH01", start, null));
        check("Reject equal start/end", !hotel.bookRoom("R101", "KH01", start, start));
        check("Reject end before start", !hotel.bookRoom("R101", "KH01", end, start));
        check("Book room successfully", hotel.bookRoom("R101", "KH01", start, end));
        // The service's static counter starts at C0 when this program is run in a fresh JVM.
        RentalContract contract = hotel.findContractById("C0");
        check("Booking creates a contract", contract != null);
        check("Booking changes room status to BOOKED", room.getRoomStatus() == roomStatus.BOOKED);
        check("Reject another booking while room is BOOKED", !hotel.bookRoom("R101", "KH02", start, end));
        check("Find ongoing contract for customer", hotel.findOnGoingContractByCustomer(customer).size() == 1
                && hotel.findOnGoingContractByCustomer(customer).get(0) == contract);

        System.out.println("\n===== 4. CHECK-IN TESTS =====");
        check("Reject check-in for unknown contract", !hotel.checkIn("NO_CONTRACT", checkIn, 1));
        check("Reject null check-in time", !hotel.checkIn("C0", null, 1));
        check("Reject check-in before booking starts",
                !hotel.checkIn("C0", start.minusMinutes(1), 1));
        check("Reject check-in after booking end", !hotel.checkIn("C0", end.plusMinutes(1), 1));
        check("Reject zero people", !hotel.checkIn("C0", checkIn, 0));
        check("Reject negative people", !hotel.checkIn("C0", checkIn, -1));
        check("Invalid check-in attempts do not set check-in time", contract.getCheckInTime() == null);
        check("Check-in succeeds with valid time and people", hotel.checkIn("C0", checkIn, 4));
        check("Check-in time is saved", checkIn.equals(contract.getCheckInTime()));
        check("Number of people is saved", contract.getRoomCapacityUsed() == 4);
        check("Reject duplicate check-in", !hotel.checkIn("C0", checkIn, 4));

        System.out.println("\n===== 5. UTILITY TESTS =====");
        check("Reject utility use for unknown contract", !hotel.useUtility("NO_CONTRACT", wifi));
        check("Reject utility use before check-in", !hotel.useUtility("C0", wifi));
        check("Reject null utility", !hotel.useUtility("C0", null));
        check("Record use of free WiFi", hotel.useUtility("C0", wifi));
        check("Record use of paid breakfast", hotel.useUtility("C0", breakfast));
        check("Both utilities recorded", contract.getUtilitiesUsed().size() == 2);
        boolean listIsUnmodifiable = false;
        try {
            contract.getUtilitiesUsed().clear();
        } catch (UnsupportedOperationException expected) {
            listIsUnmodifiable = true;
        }
        check("RentalContract returns an unmodifiable utilities list",
                listIsUnmodifiable && contract.getUtilitiesUsed().size() == 2);
        checkDouble("Utility fee excludes free WiFi", hotel.calculateUtilityFee(contract), 20.0);

        System.out.println("\n===== 6. CHECK-OUT TESTS =====");
        check("Reject null checkout time", !hotel.checkOut("C0", null));
        check("Reject checkout before check-in", !hotel.checkOut("C0", checkIn.minusMinutes(1)));
        check("Checkout fee stays zero before a successful checkout", hotel.calculateRoomFee(contract) == 0.0);
        check("Check-out succeeds", hotel.checkOut("C0", checkOut));
        check("Checkout time is saved", checkOut.equals(contract.getCheckOutTime()));
        check("Contract status becomes completed", contract.getStatus() == contractStatus.completed);
        check("Room becomes AVAILABLE after checkout", room.getRoomStatus() == roomStatus.AVAILABLE);
        check("Completed contract no longer appears in ongoing list",
                hotel.findOnGoingContractByCustomer(customer).isEmpty());
        check("Reject duplicate checkout", !hotel.checkOut("C0", checkOut));
        check("Reject utility use after checkout", !hotel.useUtility("C0", wifi));

        System.out.println("\n===== 7. FEE-CALCULATION TESTS =====");
        // Current code uses calendar-date differences: Oct 5 -> Oct 10 = 5 days.
        checkDouble("Room fee", hotel.calculateRoomFee(contract), 500.0);
        // Oct 8 -> Oct 10 = 2 calendar days * 150 per day.
        checkDouble("Overstay fee", hotel.calculateOverstayFee(contract), 300.0);
        // WiFi is free for Deluxe; breakfast costs 20.
        checkDouble("Utility fee", hotel.calculateUtilityFee(contract), 20.0);
        // 4 people - room capacity 2 = 2 excess people * 80.
        checkDouble("Over-capacity fee", hotel.calculateOverMaxCapacityFee(contract), 160.0);

        System.out.println("\n===== 8. INVOICE TESTS =====");
        String nullInvoiceOutput = captureStdout(() -> hotel.createInvoice(null, customer, paymentType.CASH));
        check("Reject invoice creation with null contract", nullInvoiceOutput.contains("Contract not found"));

        // This new contract exists but has not checked out; invoice creation must be rejected.
        check("Book second room", hotel.bookRoom("R202", "KH02", start, end));
        RentalContract unfinishedContract = hotel.findContractById("C1");
        check("Second contract exists", unfinishedContract != null);
        String unfinishedInvoiceOutput = captureStdout(
                () -> hotel.createInvoice(unfinishedContract, customer2, paymentType.CASH));
        check("Reject invoice before checkout", unfinishedInvoiceOutput.contains("Customer has not checked out"));

        String invoiceOutput = captureStdout(() -> hotel.createInvoice(contract, customer, paymentType.CASH));
        check("Invoice is printed", invoiceOutput.contains("Invoice{"));
        check("Invoice has correct room fee", invoiceOutput.contains("roomFee=500.0"));
        check("Invoice has correct overstay fee", invoiceOutput.contains("overstayFee=300.0"));
        check("Invoice utility / capacity fees are mapped correctly",
                invoiceOutput.contains("utilityFee=20.0") && invoiceOutput.contains("overMaxCapacityFee=160.0"));
        check("Invoice deposit is 10% of room fee", invoiceOutput.contains("deposit=50.0"));
        check("Invoice total is correct", invoiceOutput.contains("total=930.0"));
        check("Invoice records payment type", invoiceOutput.contains("paymentType=CASH"));

        String duplicateInvoiceOutput = captureStdout(
                () -> hotel.createInvoice(contract, customer, paymentType.CARD));
        check("Reject duplicate invoice for same contract",
                duplicateInvoiceOutput.contains("Invoice already exists for this contract"));
        check("Duplicate attempt does not print a second invoice",
                !duplicateInvoiceOutput.contains("Invoice{"));

        System.out.println("\n===== SUMMARY =====");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Total:  " + (passed + failed));
        if (failed == 0) {
            System.out.println("RESULT: ALL TESTS PASSED");
        } else {
            System.out.println("RESULT: SOME TESTS FAILED");
            throw new AssertionError(failed + " test(s) failed. See [FAIL] lines above.");
        }

        System.out.println("\nKnown gaps not asserted as passing behavior:");
        System.out.println("- addCustomer/addRoom currently do not reject null objects or duplicate IDs.");
        System.out.println("- useUtility currently does not validate utility membership in the room group's offered utilities.");
        System.out.println("- roomGroup.getUtilities() currently exposes its mutable internal list.");
        System.out.println("- Fee calculations use calendar-date differences; same-day stays currently calculate as zero room-fee days.");
    }
}