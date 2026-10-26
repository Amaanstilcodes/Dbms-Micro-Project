module com.isp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.zaxxer.hikari;
    requires org.slf4j;
    requires jbcrypt;

    // Allow FXML loader to access controllers
    opens com.isp             to javafx.fxml;
    opens com.isp.controller  to javafx.fxml;
    opens com.isp.model       to javafx.base;  // for PropertyValueFactory reflection
    opens com.isp.service     to javafx.fxml;  // MockDataService (Phase A; remove in Phase B)

    exports com.isp;
    exports com.isp.controller;
    exports com.isp.model;
    exports com.isp.dao;
    exports com.isp.util;
    exports com.isp.service;   // Phase A; remove in Phase B
}
