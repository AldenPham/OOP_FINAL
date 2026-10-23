package hotel;

import hotel.model.*;
import hotel.model.enums.*;
import hotel.model.policy.FeePolicy;
import hotel.service.HotelService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/** Standalone integration smoke test; intentionally needs no test framework. */
public final class PersistenceSmoke {
    public static void main(String[] args) throws Exception {
        Path database = Files.createTempFile("hotel-smoke-", ".db");
        try {
            HotelService first = new HotelService(database.toString());
            Customer customer = first.createCustomer("Test Customer", "000", "TEST-ID", CustomerType.NORMAL);
            RoomGroup group = first.createRoomGroup("Test Group", RoomType.STANDARD, new FeePolicy(25, 10));
            Utility laundry = first.createUtility("Laundry", 5);
            first.assignUtilityToRoomGroup(laundry.getUtilityId(), group.getGroupId(), false);
            Utility wifi = first.createUtility("Wi-Fi", 0);
            first.assignUtilityToRoomGroup(wifi.getUtilityId(), group.getGroupId(), true);
            Room room = first.createRoom("1F", 2, group, RoomStatus.AVAILABLE, 100);
            LocalDateTime start = LocalDateTime.of(2026, 10, 20, 10, 0);
            LocalDateTime end = LocalDateTime.of(2026, 10, 21, 10, 0);
            require(first.bookRoom(room.getRoomId(), customer.getId(), start, end), "booking failed");
            RentalContract contract = first.getContracts().get(0);
            require(contract.getContractId().equals("C0"), "first contract ID must be C0");
            require(first.checkIn(contract.getContractId(), start, 3), "check-in failed");
            require(first.useUtility(contract.getContractId(), laundry), "first utility usage failed");
            require(first.useUtility(contract.getContractId(), laundry), "repeated utility usage failed");
            require(first.checkOut(contract.getContractId(), LocalDateTime.of(2026, 10, 23, 11, 0)), "check-out failed");
            Invoice invoice = first.createInvoice(contract.getContractId(), customer.getId(), PaymentType.CASH);
            require(invoice != null && invoice.getInvoiceId().equals("I0"), "invoice creation/ID failed");
            require(Math.abs(invoice.getTotal() - 340.0) < 0.001, "unexpected bill total: " + (invoice == null ? "null" : invoice.getTotal()));

            HotelService second = new HotelService(database.toString());
            require(second.getCustomers().size() == 1 && second.getRoomGroups().size() == 1 && second.getRooms().size() == 1, "base records failed to reload");
            require(second.getContracts().size() == 1 && second.getInvoices().size() == 1, "contracts/invoices failed to reload");
            RentalContract reloaded = second.findContractById(contract.getContractId());
            require(reloaded != null && reloaded.getCustomer().getId().equals(customer.getId()), "customer reference failed to reload");
            require(reloaded.getUtilitiesUsed().size() == 2, "repeated utility usage failed to reload");
            require(reloaded.getStatus() == ContractStatus.COMPLETED && reloaded.getRoom().getRoomStatus() == RoomStatus.AVAILABLE, "state/status failed to reload");
            require(second.findRoomGroup(group.getGroupId()).getUtilities().size() == 2, "group/utility relationship failed to reload");
            require(second.findUtility(wifi.getUtilityId()).isFreeForGroup(second.findRoomGroup(group.getGroupId())), "free utility association failed to reload");
            require(second.createCustomer("Second Customer", "001", "TEST-ID-2", CustomerType.REGULAR).getId().equals("KH1"), "customer ID did not continue after restart");
            require(second.createRoomGroup("Second Group", RoomType.DELUXE, new FeePolicy(0, 0)).getGroupId().equals("RG1"), "room group ID did not continue after restart");
            System.out.println("PASS: persistence smoke test — save/load, object relationships, repeated utilities, billing, status transitions, and IDs across restart.");
        } finally {
            Files.deleteIfExists(database);
            Files.deleteIfExists(Path.of(database + "-shm"));
            Files.deleteIfExists(Path.of(database + "-wal"));
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
