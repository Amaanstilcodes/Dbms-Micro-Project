package com.isp.dao;

import java.util.List;

public class CustomerModels {

    public record CustomerRow(
        int customerId,
        String name,
        String email,
        String phone,
        String customerType,
        String areaName,
        String status,
        String createdAt
    ) {}

    public record ServiceAreaItem(
        int areaId,
        String areaName,
        String city,
        String pinCode
    ) {
        @Override
        public String toString() {
            return areaName + " (" + pinCode + ", " + city + ")";
        }
    }

    public record CustomerDetails(
        int customerId,
        String customerType,
        String firstName,
        String lastName,
        String companyName,
        String email,
        String phone,
        String alternatePhone,
        int areaId,
        String areaName,
        String status,
        String idProofType,
        String idProofNumber,
        String gstNumber,
        String createdAt,
        String updatedAt,
        List<SubInfo> subscriptions,
        List<InvInfo> lastInvoices,
        List<TicketInfo> tickets
    ) {
        public record SubInfo(int subId, String planName, String planType, String status, String startDate, String endDate) {}
        public record InvInfo(int invId, double totalAmount, double amountPaid, String status, String dueDate) {}
        public record TicketInfo(int ticketId, String subject, String priority, String status, String createdAt) {}
    }
}
