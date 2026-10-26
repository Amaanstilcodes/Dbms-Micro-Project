package com.isp.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class StatusBadge extends Label {

    public StatusBadge(String status) {
        String s = status != null ? status.trim() : "UNKNOWN";
        setText(s.isEmpty() ? "UNKNOWN" : s);
        setAlignment(Pos.CENTER);
        setFont(Font.font("System", FontWeight.BOLD, 12));
        getStyleClass().add("badge");
        applyVariant(s);
    }

    private void applyVariant(String status) {
        if (status == null) {
            getStyleClass().add("badge-neutral");
            return;
        }

        String s = status.toUpperCase();
        switch (s) {
            case "ACTIVE":
            case "PAID":
            case "COMPLETED":
            case "RESOLVED":
            case "NORMAL":
            case "LIGHT":
            case "AVAILABLE":
                getStyleClass().add("badge-success");
                break;

            case "PENDING":
            case "IN_PROGRESS":
            case "SCHEDULED":
            case "WARNING":
            case "BUSY":
                getStyleClass().add("badge-warning");
                break;

            case "OVERDUE":
            case "SUSPENDED":
            case "TERMINATED":
            case "CRITICAL":
            case "EXCEEDED":
            case "OVERLOADED":
            case "CANCELLED":
            case "FAULTY":
            case "BLACKLISTED":
                getStyleClass().add("badge-danger");
                break;

            case "PARTIALLY_PAID":
            case "ISSUED":
            case "IN_STOCK":
            case "ASSIGNED":
            case "OPEN":
                getStyleClass().add("badge-info");
                break;

            default:
                getStyleClass().add("badge-neutral");
                break;
        }
    }
}
