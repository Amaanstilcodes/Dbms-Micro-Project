package com.isp.ui.util;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Formatters {
    private static final NumberFormat INR_FORMAT = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public static String formatCurrency(double amount) {
        return INR_FORMAT.format(amount);
    }

    public static String formatCurrency(String amountStr) {
        try {
            double v = Double.parseDouble(amountStr.replaceAll("[^0-9.-]", ""));
            return INR_FORMAT.format(v);
        } catch (Exception e) {
            return amountStr;
        }
    }

    public static String formatDate(LocalDate date) {
        if (date == null) return "-";
        return date.format(DATE_FMT);
    }

    public static String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return "-";
        try {
            if (dateStr.length() >= 10) {
                LocalDate d = LocalDate.parse(dateStr.substring(0, 10));
                return d.format(DATE_FMT);
            }
        } catch (Exception ignored) {}
        return dateStr;
    }
}
