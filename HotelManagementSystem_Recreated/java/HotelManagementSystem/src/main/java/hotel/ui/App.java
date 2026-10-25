package hotel.ui;

import hotel.model.*;
import hotel.model.enums.*;
import hotel.model.policy.FeePolicy;
import hotel.service.HotelService;
import hotel.dao.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/** Menu-driven console matching the original project workflows and ten reports. */
public final class App {
    private static final Scanner INPUT = new Scanner(System.in);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final HotelService hotel;
    private boolean running = true;

    private App(HotelService hotel) { this.hotel = hotel; }

    public static void main(String[] args) {
        try {
            String database = args.length > 0 ? args[0] : "hotel.db";
            HotelService hotel = new HotelService(database);
            System.out.println("Database: " + hotel.getDatabasePath());
            System.out.println("Loaded persisted hotel data successfully.");
            new App(hotel).run();
        } catch (RuntimeException ex) {
            System.err.println("Could not start the hotel system: " + ex.getMessage());
        }
    }

    private void run() {
        System.out.println("==============================================");
        System.out.println("     HOTEL BOOKING SYSTEM - JAVA + SQLITE");
        System.out.println("==============================================");
        System.out.println("Date/time format: yyyy-MM-dd HH:mm (24-hour)");
        while (running) {
            printMenu(); int choice = readInt("Choose an option: "); System.out.println();
            try {
                switch (choice) {
                    case 1 -> addCustomer(); case 2 -> listCustomers(); case 3 -> addRoomGroup();
                    case 4 -> addUtility(); case 5 -> listRoomGroupsAndUtilities(); case 6 -> addRoom();
                    case 7 -> listRooms(false); case 8 -> listRooms(true); case 9 -> bookRoom();
                    case 10 -> listContracts(); case 11 -> findOngoingContracts(); case 12 -> checkIn();
                    case 13 -> useUtility(); case 14 -> checkOut(); case 15 -> previewBill();
                    case 16 -> createInvoice(); case 17 -> listInvoices(); case 18 -> runReports();
                    case 19 -> runDao();
                    case 0 -> { running = false; System.out.println("Goodbye!"); }
                    default -> System.out.println("Invalid menu option. Choose 0-19.");
                }
            } catch (RuntimeException ex) {
                System.out.println("Operation failed: " + ex.getMessage());
            }
            if (running) { System.out.println(); System.out.print("Press Enter to return to the menu..."); INPUT.nextLine(); }
        }
        INPUT.close();
    }

    private void printMenu() {
        System.out.println("\n=============== MAIN MENU ===============");
        System.out.println(" CUSTOMERS\n  1. Add customer\n  2. List customers");
        System.out.println(" ROOM SETUP\n  3. Add room group / category\n  4. Add utility and assign to room groups\n  5. List room groups and utilities\n  6. Add room\n  7. List all rooms\n  8. List available rooms only");
        System.out.println(" BOOKING OPERATIONS\n  9. Book a room\n 10. List contracts\n 11. Find customer's ongoing contracts\n 12. Check in\n 13. Use utility\n 14. Check out");
        System.out.println(" BILLING\n 15. Preview bill\n 16. Create invoice\n 17. List invoices");
        System.out.println(" REPORTS\n 18. Run all 10 reports (Stream/lambda)\n 19.DAO\n  0. Exit\n=========================================");
    }

    private void addCustomer() {
        String name = readNonBlank("Full name: "), phone = readNonBlank("Phone number: "), cccd = readNonBlank("CCCD / ID number: ");
        CustomerType type = readEnum("Customer type", CustomerType.values());
        Customer x = hotel.createCustomer(name, phone, cccd, type); System.out.println("Customer saved. ID: " + x.getId());
    }
    private void listCustomers() {
        System.out.println("--- Customers ---");
        if (hotel.getCustomers().isEmpty()) System.out.println("No customers yet.");
        hotel.getCustomers().forEach(System.out::println);
    }
    private void addRoomGroup() {
        String name = readNonBlank("Room-group name: "); RoomType type = readEnum("Room type", RoomType.values());
        double stay = readNonNegativeDouble("Overstay fee per day: "), capacity = readNonNegativeDouble("Fee per extra person over capacity: ");
        RoomGroup g = hotel.createRoomGroup(name, type, new FeePolicy(stay, capacity)); System.out.println("Room group saved. ID: " + g.getGroupId());
    }
    private void addUtility() {
        Utility u = hotel.createUtility(readNonBlank("Utility name: "), readNonNegativeDouble("Price per use: "));
        System.out.println("Utility saved. ID: " + u.getUtilityId());
        if (hotel.getRoomGroups().isEmpty()) { System.out.println("Create a room group first to assign this utility later."); return; }
        while (readYesNo("Assign utility to a room group? (y/n): ")) {
            listRoomGroupsShort(); String groupId = readNonBlank("Room-group ID: ");
            if (hotel.findRoomGroup(groupId) == null) { System.out.println("Room group not found."); continue; }
            hotel.assignUtilityToRoomGroup(u.getUtilityId(), groupId, readYesNo("Is it free for this room group? (y/n): "));
            System.out.println("Utility assignment saved.");
        }
    }
    private void listRoomGroupsShort() {
        if (hotel.getRoomGroups().isEmpty()) System.out.println("No room groups yet.");
        hotel.getRoomGroups().forEach(g -> System.out.printf("%s | %s | %s%n", g.getGroupId(), g.getGroupName(), g.getType()));
    }
    private void listRoomGroupsAndUtilities() {
        System.out.println("--- Room groups and utilities ---");
        if (hotel.getRoomGroups().isEmpty()) System.out.println("No room groups yet.");
        for (RoomGroup g : hotel.getRoomGroups()) {
            System.out.printf(Locale.ROOT, "%s | %s | %s | overstay/day: %.2f | over-capacity/person: %.2f%n", g.getGroupId(), g.getGroupName(), g.getType(), g.getFee().overstayFeePerDay(), g.getFee().overMaxCapacityFee());
            if (g.getUtilities().isEmpty()) System.out.println("  No utilities assigned.");
            g.getUtilities().forEach(u -> System.out.printf(Locale.ROOT, "  %s | %s | %.2f | free: %s%n", u.getUtilityId(), u.getUtilityName(), u.getUtilityPrice(), u.isFreeForGroup(g)));
        }
    }
    private void addRoom() {
        if (hotel.getRoomGroups().isEmpty()) { System.out.println("Create a room group first (option 3)."); return; }
        String location = readNonBlank("Room location / floor: "); int capacity = readPositiveInt("Maximum capacity: ");
        double price = readNonNegativeDouble("Price per calendar day: "); listRoomGroupsShort();
        RoomGroup group = hotel.findRoomGroup(readNonBlank("Room-group ID: "));
        if (group == null) { System.out.println("Room group not found."); return; }
        Room room = hotel.createRoom(location, capacity, group, RoomStatus.AVAILABLE, price); System.out.println("Room saved. ID: " + room.getRoomId());
    }
    private void listRooms(boolean availableOnly) {
        System.out.println(availableOnly ? "--- Available rooms ---" : "--- All rooms ---");
        List<Room> list = hotel.getRooms().stream().filter(r -> !availableOnly || r.getRoomStatus() == RoomStatus.AVAILABLE).toList();
        if (list.isEmpty()) System.out.println(availableOnly ? "No rooms currently available." : "No rooms yet.");
        list.forEach(System.out::println);
    }
    private void bookRoom() {
        if (hotel.getCustomers().isEmpty() || hotel.getRooms().isEmpty()) { System.out.println("Add a customer and a room first."); return; }
        listRooms(true); String roomId = readNonBlank("Room ID: "), customerId = readNonBlank("Customer ID: ");
        LocalDateTime start = readDateTime("Booking start (yyyy-MM-dd HH:mm): "), end = readDateTime("Booking end   (yyyy-MM-dd HH:mm): ");
        if (!hotel.bookRoom(roomId, customerId, start, end)) { System.out.println("Booking failed. Check IDs, dates, and room availability."); return; }
        System.out.println("Booking saved. Contract ID: " + hotel.getContracts().get(hotel.getContracts().size() - 1).getContractId());
    }
    private void listContracts() {
        System.out.println("--- Contracts ---");
        if (hotel.getContracts().isEmpty()) System.out.println("No contracts yet.");
        for (RentalContract c : hotel.getContracts()) {
            System.out.printf("%s | customer: %s (%s) | room: %s (%s) | %s -> %s | status: %s | check-in: %s | check-out: %s%n",
                c.getContractId(), c.getCustomer().getId(), c.getCustomer().getName(), c.getRoom().getRoomId(), c.getRoom().getRoomType(),
                c.getStartTime().format(DATE_FORMAT), c.getEndTime().format(DATE_FORMAT), c.getStatus(), formatDate(c.getCheckInTime()), formatDate(c.getCheckOutTime()));
        }
    }
    private RentalContract promptContract() {
        RentalContract c = hotel.findContractById(readNonBlank("Contract ID: "));
        if (c == null) System.out.println("Contract not found."); return c;
    }
    private void findOngoingContracts() {
        Customer c = hotel.findCustomer(readNonBlank("Customer ID: "));
        if (c == null) { System.out.println("Customer not found."); return; }
        List<RentalContract> list = hotel.findOngoingContractsByCustomer(c);
        if (list.isEmpty()) System.out.println("This customer has no ongoing contracts."); list.forEach(System.out::println);
    }
    private void checkIn() {
        RentalContract c = promptContract(); if (c == null) return;
        LocalDateTime time = readDateTime("Check-in time (yyyy-MM-dd HH:mm): "); int people = readPositiveInt("Number of people: ");
        System.out.println(hotel.checkIn(c.getContractId(), time, people) ? "Check-in saved." : "Check-in failed. Validate contract, time and people count.");
    }
    private void useUtility() {
        RentalContract c = promptContract(); if (c == null) return;
        if (hotel.getUtilities().isEmpty()) { System.out.println("No utilities yet."); return; }
        hotel.getUtilities().forEach(System.out::println); Utility u = hotel.findUtility(readNonBlank("Utility ID: "));
        System.out.println(hotel.useUtility(c.getContractId(), u) ? "Utility use saved." : "Could not record utility use.");
    }
    private void checkOut() {
        RentalContract c = promptContract(); if (c == null) return;
        LocalDateTime time = readDateTime("Check-out time (yyyy-MM-dd HH:mm): ");
        System.out.println(hotel.checkOut(c.getContractId(), time) ? "Check-out saved; room is now available." : "Check-out failed. Validate check-in and time.");
    }
    private void previewBill() {
        RentalContract c = promptContract(); if (c == null) return;
        if (c.getCheckOutTime() == null) { System.out.println("A final bill is available after check-out."); return; }
        printBill(c);
    }
    private double printBill(RentalContract c) {
        double room = hotel.calculateRoomFee(c), stay = hotel.calculateOverstayFee(c), utility = hotel.calculateUtilityFee(c), capacity = hotel.calculateOverCapacityFee(c);
        double deposit = room * 0.10, total = room + stay + utility + capacity - deposit;
        System.out.printf(Locale.ROOT, "Room fee:                 %.2f%nOverstay fee:             %.2f%nUtility fee:              %.2f%nOver-capacity fee:        %.2f%nDeposit deduction (10%%): -%.2f%nTOTAL DUE:                %.2f%n", room, stay, utility, capacity, deposit, total);
        return total;
    }
    private void createInvoice() {
        RentalContract c = promptContract(); if (c == null) return;
        if (c.getCheckOutTime() == null) { System.out.println("Customer must check out first."); return; }
        if (hotel.getInvoices().stream().anyMatch(i -> i.getContract().getContractId().equals(c.getContractId()))) { System.out.println("Invoice already exists for this contract."); return; }
        listCustomers(); Customer payer = hotel.findCustomer(readNonBlank("Payer customer ID: "));
        if (payer == null) { System.out.println("Payer not found."); return; }
        PaymentType method = readEnum("Payment method", PaymentType.values()); Invoice invoice = hotel.createInvoice(c.getContractId(), payer.getId(), method);
        System.out.println(invoice == null ? "Invoice was not created." : "Invoice saved: " + invoice);
    }
    private void listInvoices() {
        System.out.println("--- Invoices ---"); 
        if (hotel.getInvoices().isEmpty()) System.out.println("No invoices yet."); 
        hotel.getInvoices().forEach(System.out::println);
    }

    private void runReports() {
        List<RoomType> types = List.of(RoomType.values()); List<PaymentType> paymentTypes = List.of(PaymentType.values());
        List<RentalContract> contracts = hotel.getContracts(); List<Invoice> invoices = hotel.getInvoices();
        System.out.println("========================================\n             HOTEL REPORTS\n========================================");
        System.out.println("\n1. Available rooms by room type");
        types.forEach(t -> System.out.printf("  %-12s: %d room(s)%n", t, hotel.getRooms().stream().filter(r -> r.getRoomStatus() == RoomStatus.AVAILABLE && r.getRoomType() == t).count()));

        Map<RoomType, Long> bookings = contracts.stream().collect(Collectors.groupingBy(c -> c.getRoom().getRoomType(), Collectors.counting()));
        System.out.println("\n2. Most booked room type"); long max = types.stream().mapToLong(t -> bookings.getOrDefault(t, 0L)).max().orElse(0);
        if (max == 0) System.out.println("  No bookings yet."); else types.stream().filter(t -> bookings.getOrDefault(t, 0L) == max).forEach(t -> System.out.println("  " + t + ": " + max + " booking(s)"));

        Map<String, Long> stays = contracts.stream().filter(c -> c.getStatus() == ContractStatus.COMPLETED).collect(Collectors.groupingBy(c -> c.getCustomer().getId(), Collectors.counting()));
        System.out.println("\n3. Number of customer return visits"); List<Map.Entry<String, Long>> returned = stays.entrySet().stream().filter(e -> e.getValue() > 1).sorted(Map.Entry.<String,Long>comparingByValue().reversed()).toList();
        if (returned.isEmpty()) System.out.println("  No customer has returned for a second completed stay yet.");
        returned.forEach(e -> System.out.printf("  %s | completed stays: %d | return visits: %d%n", e.getKey(), e.getValue(), e.getValue() - 1));

        System.out.println("\n4. Revenue by payment method");
        paymentTypes.forEach(p -> System.out.printf(Locale.ROOT, "  %-8s: %.2f%n", p, invoices.stream().filter(i -> i.getPaymentType() == p).mapToDouble(Invoice::getTotal).sum()));

        System.out.println("\n5. Utilities by room group");
        if (hotel.getRoomGroups().isEmpty()) System.out.println("  No room groups yet.");
        for (RoomGroup g : hotel.getRoomGroups()) {
            String desc = g.getUtilities().stream().map(u -> u.getUtilityName() + (u.isFreeForGroup(g) ? " (free)" : " (paid)")).collect(Collectors.joining(", "));
            System.out.printf("  %s (%s): %s%n", g.getGroupId(), g.getType(), desc.isBlank() ? "no utilities" : desc);
        }

        System.out.println("\n6. Least booked room type"); long min = types.stream().mapToLong(t -> bookings.getOrDefault(t, 0L)).min().orElse(0);
        types.stream().filter(t -> bookings.getOrDefault(t, 0L) == min).forEach(t -> System.out.println("  " + t + ": " + min + " booking(s)"));

        System.out.println("\n7. Revenue contributed by each room type");
        types.forEach(t -> System.out.printf(Locale.ROOT, "  %-12s: %.2f%n", t, invoices.stream().filter(i -> i.getContract().getRoom().getRoomType() == t).mapToDouble(Invoice::getTotal).sum()));

        System.out.println("\n8. Revenue by quarter (based on check-out date)");
        Map<String, Double> quarterly = invoices.stream().filter(i -> i.getContract().getCheckOutTime() != null).collect(Collectors.groupingBy(i -> {
            LocalDateTime d = i.getContract().getCheckOutTime(); return d.getYear() + "-Q" + ((d.getMonthValue() - 1) / 3 + 1);
        }, TreeMap::new, Collectors.summingDouble(Invoice::getTotal)));
        if (quarterly.isEmpty()) System.out.println("  No invoices yet."); quarterly.forEach((q, total) -> System.out.printf(Locale.ROOT, "  %s: %.2f%n", q, total));

        System.out.println("\n9. Total hotel revenue (created invoices only)"); System.out.printf(Locale.ROOT, "  Total: %.2f%n", invoices.stream().mapToDouble(Invoice::getTotal).sum());

        System.out.println("\n10. Customer with the most return visits"); long most = stays.values().stream().mapToLong(Long::longValue).max().orElse(0);
        if (most <= 1) System.out.println("  No returning customer found yet (a return requires at least 2 completed stays).");
        else stays.entrySet().stream().filter(e -> e.getValue() == most).forEach(e -> { Customer c = hotel.findCustomer(e.getKey()); System.out.printf("  %s (%s) | completed stays: %d | return visits: %d%n", c == null ? e.getKey() : c.getName(), e.getKey(), e.getValue(), e.getValue() - 1); });

        // 11. Customer with the most contracts
        System.out.println("\n11. Customer with the most contracts");

        Map<String, List<RentalContract>> contractsByCustomer = contracts.stream()
            .collect(Collectors.groupingBy(c -> c.getCustomer().getId()));

        long maxContracts = contractsByCustomer.values().stream()
            .mapToLong(List::size)
            .max()
            .orElse(0);

        contractsByCustomer.forEach((id, list) -> {
            if (list.size() == maxContracts && maxContracts > 0) {
                Customer c = list.get(0).getCustomer();
                System.out.printf(
                    "  %s (%s): %d contract(s)%n",
                    c.getName(), id, list.size()
                );
            }
        });


        // 12. Rooms with above-average capacity
        System.out.println("\n12. Rooms with above-average capacity");

        double avgCapacity = hotel.getRooms().stream()
            .mapToInt(Room::getRoomCapacity)
            .average()
            .orElse(0);

        hotel.getRooms().stream()
            .filter(r -> r.getRoomCapacity() > avgCapacity)
            .sorted(Comparator.comparingInt(Room::getRoomCapacity).reversed())
            .forEach(r -> System.out.printf(
                "  Room %s: capacity %d%n",
                r.getRoomId(), r.getRoomCapacity()
            ));


        // 13. Contracts by customer type
        System.out.println("\n13. Contracts by customer type");

        contracts.stream()
            .collect(Collectors.groupingBy(
                c -> c.getCustomer().getType(),
                Collectors.counting()
            ))
            .entrySet()
            .stream()
            .sorted(Map.Entry.<CustomerType, Long>comparingByValue().reversed())
            .forEach(e -> System.out.printf(
                "  %s: %d contract(s)%n",
                e.getKey(), e.getValue()
            ));


        // 14. Utilities never used
        System.out.println("\n14. Utilities never used");

        List<Utility> unusedUtilities = hotel.getUtilities().stream()
            .filter(u -> contracts.stream()
                .noneMatch(c -> c.getUtilitiesUsed().stream()
                    .anyMatch(used ->
                        used.getUtilityId().equals(u.getUtilityId())
                    )
                )
            )
            .toList();

        if (unusedUtilities.isEmpty()) {
            System.out.println("  All utilities have been used.");
        }

        unusedUtilities.forEach(u -> System.out.printf(
            "  %s | %s%n", u.getUtilityId(), u.getUtilityName()
        ));


        // 15. Average invoice by payer customer type
        System.out.println("\n15. Average invoice by payer customer type");

        invoices.stream()
            .collect(Collectors.groupingBy(
                i -> i.getPayer().getType(),
                Collectors.averagingDouble(Invoice::getTotal)
            ))
            .entrySet()
            .stream()
            .sorted(Map.Entry.<CustomerType, Double>comparingByValue().reversed())
            .forEach(e -> System.out.printf(
                Locale.ROOT, "  %s: average %.2f%n",
                e.getKey(), e.getValue()
            ));


        // 16. Longest booking duration
        System.out.println("\n16. Longest booking duration");

        Map<RentalContract, Double> bookingDurations = contracts.stream()
            .collect(Collectors.toMap(
                c -> c,
                c -> (double) java.time.Duration.between(
                    c.getStartTime(), c.getEndTime()
                ).toSeconds() / 86400.0
            ));

        double longest = bookingDurations.values().stream()
            .mapToDouble(Double::doubleValue)
            .max()
            .orElse(0);

        bookingDurations.forEach((c, days) -> {
            if (Double.compare(days, longest) == 0) {
                System.out.printf(
                    Locale.ROOT, "  %s | %s | %.2f day(s)%n",
                    c.getContractId(), c.getCustomer().getName(), days
                );
            }
        });


        // 17. Room booking ranking
        System.out.println("\n17. Room booking ranking");

        hotel.getRooms().stream()
            .map(r -> new AbstractMap.SimpleEntry<>(
                r,
                contracts.stream()
                    .filter(c -> c.getRoom().getRoomId().equals(r.getRoomId()))
                    .count()
            ))
            .sorted(
                Map.Entry.<Room, Long>comparingByValue()
                    .reversed()
                    .thenComparing(e -> e.getKey().getRoomId())
            )
            .forEach(e -> System.out.printf(
                "  Room %s: %d booking(s)%n",
                e.getKey().getRoomId(), e.getValue()
            ));


        // 18. Customers with ongoing contracts
        System.out.println("\n18. Customers with ongoing contracts");

        contracts.stream()
            .filter(c -> c.getStatus() == ContractStatus.ON_GOING)
            .map(RentalContract::getCustomer)
            .collect(Collectors.toMap(
                Customer::getId,
                c -> c,
                (first, second) -> first
            ))
            .values()
            .forEach(c -> System.out.printf(
                "  %s | %s%n", c.getId(), c.getName()
            ));


        // 19. Utility usage by contract
        System.out.println("\n19. Utility usage by contract");

        contracts.stream()
            .sorted(Comparator.comparingInt(
                (RentalContract c) -> c.getUtilitiesUsed().size()
            ).reversed())
            .forEach(c -> System.out.printf(
                "  %s | %s: %d utility use(s)%n",
                c.getContractId(),
                c.getCustomer().getName(),
                c.getUtilitiesUsed().size()
            ));


        // 20. Room types with all rooms available
        System.out.println("\n20. Room types with all rooms available");

        types.stream()
            .filter(t -> hotel.getRooms().stream()
                .anyMatch(r -> r.getRoomType() == t))
            .filter(t -> hotel.getRooms().stream()
                .filter(r -> r.getRoomType() == t)
                .allMatch(r -> r.getRoomStatus() == RoomStatus.AVAILABLE))
            .forEach(System.out::println);
    
    }

    private void runDao() {
        System.out.println("=== DAO REPORTS ===");

        var repository = hotel.getRepository();

        new hotel.dao.AvailableRoomDAO(repository).execute();
        new hotel.dao.CustomerReturnFrequencyDAO(repository).execute();
        new hotel.dao.CustomerTotalSpendDAO(repository).execute();
        new hotel.dao.LeastBookedRoomTypeDAO(repository).execute();
        new hotel.dao.MostRentedRoomTypeDAO(repository).execute();
        new hotel.dao.PaymentMethodRevenueDAO(repository).execute();
        new hotel.dao.QuarterlyRevenueDAO(repository).execute();
        new hotel.dao.RoomTypeRevenueDAO(repository).execute();
        new hotel.dao.TopLoyalCustomerDAO(repository).execute();

        String roomGroupId = readLine("Enter room group ID: ");
        new hotel.dao.RoomGroupUtilityDAO(repository).execute(roomGroupId);
    }
    private static String readLine(String prompt) { System.out.print(prompt); return INPUT.nextLine().trim(); }
    private static String readNonBlank(String prompt) { while (true) { String value = readLine(prompt); if (!value.isBlank()) return value; System.out.println("Value cannot be blank."); } }
    private static int readInt(String prompt) { while (true) { try { return Integer.parseInt(readLine(prompt)); } catch (NumberFormatException ex) { System.out.println("Please enter a whole number."); } } }
    private static int readPositiveInt(String prompt) { while (true) { int x = readInt(prompt); if (x > 0) return x; System.out.println("Value must be greater than zero."); } }
    private static double readNonNegativeDouble(String prompt) { while (true) { try { double x = Double.parseDouble(readLine(prompt)); if (Double.isFinite(x) && x >= 0) return x; } catch (NumberFormatException ignored) { } System.out.println("Enter a valid non-negative number; use a dot for decimals."); } }
    private static boolean readYesNo(String prompt) { while (true) { String x = readLine(prompt).toLowerCase(Locale.ROOT); if (x.equals("y") || x.equals("yes")) return true; if (x.equals("n") || x.equals("no")) return false; System.out.println("Enter y or n."); } }
    private static LocalDateTime readDateTime(String prompt) { while (true) { try { return LocalDateTime.parse(readNonBlank(prompt), DATE_FORMAT); } catch (DateTimeParseException ex) { System.out.println("Use yyyy-MM-dd HH:mm, e.g. 2026-10-21 14:30."); } } }
    private static <E extends Enum<E>> E readEnum(String prompt, E[] values) { System.out.println(prompt + ":"); for (int i = 0; i < values.length; i++) System.out.printf("  %d. %s%n", i + 1, values[i]); while (true) { int n = readInt("Select number: "); if (n >= 1 && n <= values.length) return values[n - 1]; System.out.println("Choose a number from 1 to " + values.length + "."); } }
    private static String formatDate(LocalDateTime d) { return d == null ? "-" : d.format(DATE_FORMAT); }
}
