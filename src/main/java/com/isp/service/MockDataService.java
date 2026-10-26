package com.isp.service;

import com.isp.model.Customer;
import com.isp.model.Plan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * MockDataService — temporary, in-memory data store used while
 * the backend DAO implementations are not yet available.
 *
 * <p><strong>Phase A:</strong> Controllers call {@code MockDataService.getCustomers()}.
 * <strong>Phase B:</strong> Replace each call with the real
 * {@code DAOFactory.getCustomerDAO().findAll()} — then delete this class.</p>
 *
 * <p>Data matches {@code V003__seed_data.sql} so integration tests can
 * cross-reference expected values against the real database.</p>
 *
 * <p><strong>WARNING:</strong> This class must NOT be deployed to production.
 * It is purely a frontend development scaffold.</p>
 */
public class MockDataService {

    // =====================================================================
    // In-memory customer store (simulates DB table state)
    // =====================================================================

    /** Auto-increment counter for new customers added during this session. */
    private static final AtomicInteger nextId = new AtomicInteger(21); // seed has IDs 1-20

    /** The live list of customers — mutated by insert/update/delete operations. */
    private static final List<Customer> CUSTOMERS = new ArrayList<>(buildInitialCustomers());

    // =====================================================================
    // Customer operations
    // =====================================================================

    /** Returns a snapshot copy of all customers. */
    public static List<Customer> getCustomers() {
        return new ArrayList<>(CUSTOMERS);
    }

    /**
     * Searches by first name, last name, or email (case-insensitive).
     * Mirrors {@code CustomerDAO.search(String query)}.
     */
    public static List<Customer> searchCustomers(String query) {
        if (query == null || query.isBlank()) return getCustomers();
        String q = query.strip().toLowerCase();
        return CUSTOMERS.stream()
                .filter(c -> c.getFirstName().toLowerCase().contains(q)
                          || c.getLastName().toLowerCase().contains(q)
                          || c.getEmail().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    /**
     * Inserts a customer and returns the generated ID.
     * Mirrors {@code CustomerDAO.insert(Customer)}.
     */
    public static int insertCustomer(Customer c) {
        int id = nextId.getAndIncrement();
        c.setCustomerId(id);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        CUSTOMERS.add(c);
        return id;
    }

    /**
     * Updates an existing customer in place.
     * Mirrors {@code CustomerDAO.update(Customer)}.
     */
    public static void updateCustomer(Customer updated) {
        for (int i = 0; i < CUSTOMERS.size(); i++) {
            if (CUSTOMERS.get(i).getCustomerId() == updated.getCustomerId()) {
                updated.setUpdatedAt(LocalDateTime.now());
                CUSTOMERS.set(i, updated);
                return;
            }
        }
    }

    /**
     * Removes a customer by ID.
     * Mirrors {@code CustomerDAO.deleteById(int)}.
     *
     * @throws IllegalStateException if a business rule would block deletion
     *         (in mock mode: customers with ID 1 or 4 simulate outstanding balance)
     */
    public static void deleteCustomer(int customerId) {
        // Simulate the DB trigger: customer 3 (Suresh) has overdue invoices
        if (customerId == 3) {
            throw new IllegalStateException(
                    "Cannot delete customer with outstanding balance. Clear invoices first.");
        }
        CUSTOMERS.removeIf(c -> c.getCustomerId() == customerId);
    }

    /** Returns active customer count for dashboard KPI. */
    public static int countActiveCustomers() {
        return (int) CUSTOMERS.stream()
                .filter(c -> "ACTIVE".equals(c.getStatus()))
                .count();
    }

    // =====================================================================
    // Dashboard KPI stubs (replace each with real DAO call in Phase B)
    // =====================================================================

    public static int getActiveSubscriptionsCount()  { return 18; }
    public static String getMonthlyRevenue()         { return "45,231"; }
    public static int getOverdueInvoicesCount()      { return 3; }
    public static int getOpenTicketsCount()          { return 6; }

    // =====================================================================
    // Service areas (used by Customer form dropdown)
    // Phase B: replace with ServiceAreaDAO.findAll()
    // =====================================================================

    /** Simple holder for area data — avoids needing a full ServiceArea model. */
    public record AreaOption(int areaId, String displayName) {
        @Override public String toString() { return displayName; }
    }

    public static List<AreaOption> getServiceAreas() {
        return List.of(
            new AreaOption(1,  "Koramangala, Bangalore"),
            new AreaOption(2,  "Indiranagar, Bangalore"),
            new AreaOption(3,  "Whitefield, Bangalore"),
            new AreaOption(4,  "HSR Layout, Bangalore"),
            new AreaOption(5,  "Bandra West, Mumbai"),
            new AreaOption(6,  "Andheri East, Mumbai"),
            new AreaOption(7,  "Powai, Mumbai"),
            new AreaOption(8,  "Connaught Place, Delhi"),
            new AreaOption(9,  "Lajpat Nagar, Delhi"),
            new AreaOption(10, "Sector 14, Gurgaon")
        );
    }

    /** Returns the display name for a given areaId (used when building Customer objects). */
    public static String getAreaName(int areaId) {
        return getServiceAreas().stream()
                .filter(a -> a.areaId() == areaId)
                .map(AreaOption::displayName)
                .findFirst()
                .orElse("Unknown Area");
    }

    // =====================================================================
    // Plans (used by Subscription form dropdown — Phase D)
    // =====================================================================

    public static List<Plan> getPlans() {
        return List.of(
            makePlan(1, "Bronze 30",    "BROADBAND",  30,   10,  100,  "399.00"),
            makePlan(2, "Silver 100",   "BROADBAND",  100,  40,  250,  "699.00"),
            makePlan(3, "Gold 200",     "FIBER",      200,  100, null, "999.00"),
            makePlan(4, "Platinum 500", "FIBER",      500,  250, null, "1499.00"),
            makePlan(5, "Business Pro", "LEASED_LINE",1000,1000, null, "4999.00"),
            makePlan(6, "Wireless 20",  "WIRELESS",   20,   5,   75,  "299.00")
        );
    }

    // =====================================================================
    // Internal helpers
    // =====================================================================

    private static Plan makePlan(int id, String name, String type,
                                  int speed, int upload, Integer cap, String price) {
        Plan p = new Plan();
        p.setPlanId(id);
        p.setPlanName(name);
        p.setPlanType(type);
        p.setSpeedMbps(speed);
        p.setUploadMbps(upload);
        p.setDataCapGb(cap);
        p.setPrice(new BigDecimal(price));
        p.setBillingCycle("MONTHLY");
        p.setActive(true);
        return p;
    }

    /** Builds the initial customer list matching V003__seed_data.sql (first 10 customers). */
    private static List<Customer> buildInitialCustomers() {
        List<Customer> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        list.add(make(1,  "INDIVIDUAL", "Arjun",   "Mehta",     null,                    "arjun.mehta@email.com",     "9811111111", 1, "ACTIVE",     "AADHAAR", "XXXX-XXXX-1001", null, now));
        list.add(make(2,  "INDIVIDUAL", "Kavya",   "Reddy",     null,                    "kavya.reddy@email.com",     "9811111112", 2, "ACTIVE",     "PAN",     "ABCDE1001F",     null, now));
        list.add(make(3,  "INDIVIDUAL", "Suresh",  "Nair",      null,                    "suresh.nair@email.com",     "9811111113", 1, "ACTIVE",     "AADHAAR", "XXXX-XXXX-1003", null, now));
        list.add(make(4,  "BUSINESS",   "Pooja",   "Gupta",     "TechVentures Pvt Ltd",  "pooja.gupta@techventures.in","9811111114",3, "ACTIVE",     "AADHAAR", "XXXX-XXXX-1004", "27AABCT1234C1ZK", now));
        list.add(make(5,  "INDIVIDUAL", "Rahul",   "Joshi",     null,                    "rahul.joshi@email.com",     "9811111115", 4, "ACTIVE",     "PASSPORT","P1234567",       null, now));
        list.add(make(6,  "INDIVIDUAL", "Anita",   "Desai",     null,                    "anita.desai@email.com",     "9811111116", 5, "ACTIVE",     "AADHAAR", "XXXX-XXXX-1006", null, now));
        list.add(make(7,  "INDIVIDUAL", "Deepak",  "Shah",      null,                    "deepak.shah@email.com",     "9811111117", 6, "INACTIVE",   "PAN",     "ABCDE1007F",     null, now));
        list.add(make(8,  "BUSINESS",   "Meena",   "Kapoor",    "Kapoor Enterprises",    "meena@kapoorenterprises.com","9811111118",7, "ACTIVE",     "AADHAAR", "XXXX-XXXX-1008", null, now));
        list.add(make(9,  "INDIVIDUAL", "Vikash",  "Tiwari",    null,                    "vikash.tiwari@email.com",   "9811111119", 8, "ACTIVE",     "AADHAAR", "XXXX-XXXX-1009", null, now));
        list.add(make(10, "INDIVIDUAL", "Sneha",   "Pillai",    null,                    "sneha.pillai@email.com",    "9811111120", 9, "ACTIVE",     "PAN",     "ABCDE1010F",     null, now));
        list.add(make(14, "INDIVIDUAL", "Sanjay",  "Malhotra",  null,                    "sanjay.malhotra@email.com", "9811111124", 4, "BLACKLISTED","AADHAAR", "XXXX-XXXX-1014", null, now));

        // Resolve areaName for display
        list.forEach(c -> c.setAreaName(getAreaName(c.getAreaId())));
        return list;
    }

    private static Customer make(int id, String type, String first, String last, String company,
                                  String email, String phone, int areaId, String status,
                                  String idType, String idNum, String gst, LocalDateTime ts) {
        Customer c = new Customer();
        c.setCustomerId(id);
        c.setCustomerType(type);
        c.setFirstName(first);
        c.setLastName(last);
        c.setCompanyName(company);
        c.setEmail(email);
        c.setPhone(phone);
        c.setAreaId(areaId);
        c.setStatus(status);
        c.setIdProofType(idType);
        c.setIdProofNumber(idNum);
        c.setGstNumber(gst);
        c.setCreatedAt(ts);
        c.setUpdatedAt(ts);
        return c;
    }
}
