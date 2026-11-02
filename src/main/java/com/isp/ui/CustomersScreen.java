package com.isp.ui;

import com.isp.Session;
import com.isp.dao.CustomerDao;
import com.isp.dao.CustomerModels;
import com.isp.ui.components.ConfirmDialog;
import com.isp.ui.components.EmptyState;
import com.isp.ui.components.FormField;
import com.isp.ui.components.StatusBadge;
import com.isp.ui.components.ToastManager;
import com.isp.ui.util.Formatters;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomersScreen extends VBox {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Customers-Worker");
        t.setDaemon(true);
        return t;
    });

    private final StackPane tableContainer;
    private final TextField searchField;
    private final ComboBox<String> statusFilter;
    private final ComboBox<String> typeFilter;

    // Action buttons
    private final Button addBtn;
    private final Button viewBtn;
    private final Button editBtn;
    private final Button deactivateBtn;
    private final Button exportBtn;

    // Table & Data
    private final TableView<CustomerModels.CustomerRow> tableView;
    private final ObservableList<CustomerModels.CustomerRow> tableRows = FXCollections.observableArrayList();
    private final ProgressIndicator loadingSpinner;
    private final EmptyState emptyState;
    private final VBox errorBox;
    private final Label errorMsgLbl;
    private final Button retryBtn;

    private CustomerModels.CustomerRow selectedCustomer = null;
    private List<CustomerModels.ServiceAreaItem> cachedAreas = new ArrayList<>();

    public CustomersScreen() {
        setSpacing(16);
        setPadding(new Insets(24));
        setFillWidth(true);

        String role = Session.getRole();
        if (role == null) role = "SUPPORT_AGENT";

        boolean isAdmin = "ADMIN".equals(role);
        boolean isSupport = "SUPPORT_AGENT".equals(role);
        boolean isBilling = "BILLING_CLERK".equals(role);

        // Header Row: Title & Action Button
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Customer Directory");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Manage active broadband subscribers and corporate client accounts");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        addBtn = new Button(" Register Customer");
        addBtn.getStyleClass().add("btn-primary");
        FontIcon userPlusIcon = new FontIcon("fas-user-plus");
        userPlusIcon.setIconSize(13);
        addBtn.setGraphic(userPlusIcon);
        addBtn.setOnAction(e -> openCustomerFormDialog(null));

        // RBAC: Show Add only for ADMIN and SUPPORT_AGENT
        setWidgetVisible(addBtn, isAdmin || isSupport);

        header.getChildren().addAll(titleBox, headerSpacer, addBtn);

        // Toolbar Card: Search, Filters, Clear, Export, Edit, Deactivate
        HBox toolbar = new HBox(10);
        toolbar.getStyleClass().add("toolbar-card");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        FontIcon searchIcon = new FontIcon("fas-search");
        searchIcon.setIconSize(13);
        searchIcon.setStyle("-fx-icon-color: -color-muted;");

        searchField = new TextField();
        searchField.setPromptText("Search name, email, phone (live)...");
        searchField.setPrefWidth(240);

        // Debounced search on typing (300ms)
        PauseTransition debounce = new PauseTransition(Duration.millis(300));
        debounce.setOnFinished(e -> loadCustomers());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> debounce.playFromStart());

        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All Statuses", "ACTIVE", "INACTIVE", "BLACKLISTED");
        statusFilter.setValue("All Statuses");
        statusFilter.setOnAction(e -> loadCustomers());

        typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("All Types", "INDIVIDUAL", "BUSINESS");
        typeFilter.setValue("All Types");
        typeFilter.setOnAction(e -> loadCustomers());

        Button clearBtn = new Button("Clear Filters");
        clearBtn.getStyleClass().add("btn-ghost");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            statusFilter.setValue("All Statuses");
            typeFilter.setValue("All Types");
            loadCustomers();
        });

        exportBtn = new Button("Export CSV");
        exportBtn.getStyleClass().add("btn-secondary");
        FontIcon exportIcon = new FontIcon("fas-file-csv");
        exportIcon.setIconSize(13);
        exportBtn.setGraphic(exportIcon);
        exportBtn.setOnAction(e -> exportFilteredCsv());

        Region toolbarSpacer = new Region();
        HBox.setHgrow(toolbarSpacer, Priority.ALWAYS);

        viewBtn = new Button("View Account");
        viewBtn.getStyleClass().add("btn-secondary");
        FontIcon eyeIcon = new FontIcon("fas-id-card");
        eyeIcon.setIconSize(13);
        viewBtn.setGraphic(eyeIcon);
        viewBtn.setDisable(true);
        viewBtn.setOnAction(e -> {
            if (selectedCustomer != null) openDetailsDialog(selectedCustomer.customerId());
        });

        editBtn = new Button("Edit Account");
        editBtn.getStyleClass().add("btn-secondary");
        FontIcon editIcon = new FontIcon("fas-edit");
        editIcon.setIconSize(13);
        editBtn.setGraphic(editIcon);
        editBtn.setDisable(true);
        editBtn.setOnAction(e -> {
            if (selectedCustomer != null) openCustomerFormDialog(selectedCustomer.customerId());
        });

        deactivateBtn = new Button("Deactivate");
        deactivateBtn.getStyleClass().add("btn-danger");
        FontIcon trashIcon = new FontIcon("fas-user-slash");
        trashIcon.setIconSize(13);
        deactivateBtn.setGraphic(trashIcon);
        deactivateBtn.setDisable(true);
        deactivateBtn.setOnAction(e -> {
            if (selectedCustomer != null) openDeactivateConfirm(selectedCustomer);
        });

        // RBAC enforcement on toolbar action buttons
        setWidgetVisible(editBtn, isAdmin || isSupport);
        setWidgetVisible(deactivateBtn, isAdmin);

        toolbar.getChildren().addAll(
            searchIcon, searchField, statusFilter, typeFilter, clearBtn, exportBtn,
            toolbarSpacer, viewBtn, editBtn, deactivateBtn
        );

        // Table View Construction
        tableView = new TableView<>();
        tableView.setItems(tableRows);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        TableColumn<CustomerModels.CustomerRow, String> idCol = new TableColumn<>("ID");
        idCol.setMinWidth(60);
        idCol.setMaxWidth(80);
        idCol.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().customerId()));

        TableColumn<CustomerModels.CustomerRow, String> nameCol = new TableColumn<>("Subscriber Name");
        nameCol.setMinWidth(160);
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name()));

        TableColumn<CustomerModels.CustomerRow, String> emailCol = new TableColumn<>("Email Address");
        emailCol.setMinWidth(200);
        emailCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().email()));

        TableColumn<CustomerModels.CustomerRow, String> phoneCol = new TableColumn<>("Phone Number");
        phoneCol.setMinWidth(120);
        phoneCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().phone()));

        TableColumn<CustomerModels.CustomerRow, String> typeCol = new TableColumn<>("Type");
        typeCol.setMinWidth(110);
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().customerType()));

        TableColumn<CustomerModels.CustomerRow, String> areaCol = new TableColumn<>("Service Area");
        areaCol.setMinWidth(130);
        areaCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().areaName()));

        TableColumn<CustomerModels.CustomerRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setMinWidth(110);
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status()));
        statusCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    StatusBadge badge = new StatusBadge(item);
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                    setText(null);
                }
            }
        });

        TableColumn<CustomerModels.CustomerRow, String> dateCol = new TableColumn<>("Registered");
        dateCol.setMinWidth(110);
        dateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().createdAt()));

        tableView.getColumns().addAll(idCol, nameCol, emailCol, phoneCol, typeCol, areaCol, statusCol, dateCol);

        // Row Selection Listener & Double Click for Details
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedCustomer = newVal;
            boolean hasSel = newVal != null;
            viewBtn.setDisable(!hasSel);
            editBtn.setDisable(!hasSel);
            deactivateBtn.setDisable(!hasSel || "INACTIVE".equalsIgnoreCase(newVal.status()));
        });

        tableView.setRowFactory(tv -> {
            TableRow<CustomerModels.CustomerRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    openDetailsDialog(row.getItem().customerId());
                }
            });
            return row;
        });

        // Loading, Empty, and Error States inside Table Container
        loadingSpinner = new ProgressIndicator();
        loadingSpinner.setMaxSize(40, 40);

        emptyState = new EmptyState("No Customers Found", "No subscriber records match your filter criteria.", "fas-user-friends");
        emptyState.setVisible(false);

        errorBox = new VBox(10);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setStyle("-fx-background-color: -color-danger-bg; -fx-padding: 16; -fx-background-radius: 10; -fx-border-color: -color-danger; -fx-border-radius: 10;");
        errorBox.setVisible(false);
        errorBox.setManaged(false);

        errorMsgLbl = new Label();
        errorMsgLbl.setStyle("-fx-text-fill: -color-danger; -fx-font-weight: 600;");

        retryBtn = new Button(" Retry");
        retryBtn.getStyleClass().add("btn-danger");
        FontIcon retryIcon = new FontIcon("fas-redo");
        retryIcon.setIconSize(12);
        retryBtn.setGraphic(retryIcon);
        retryBtn.setOnAction(e -> loadCustomers());
        errorBox.getChildren().addAll(errorMsgLbl, retryBtn);

        tableContainer = new StackPane(tableView, emptyState, loadingSpinner, errorBox);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        VBox tableCard = new VBox(tableContainer);
        tableCard.getStyleClass().add("card");
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        tableCard.setFillWidth(true);

        getChildren().addAll(header, toolbar, tableCard);

        // Preload service areas and load customer list
        loadServiceAreas();
        loadCustomers();
    }

    private void setWidgetVisible(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void loadServiceAreas() {
        Task<List<CustomerModels.ServiceAreaItem>> task = new Task<>() {
            @Override
            protected List<CustomerModels.ServiceAreaItem> call() throws Exception {
                return CustomerDao.loadAreas();
            }
        };
        task.setOnSucceeded(e -> cachedAreas = task.getValue());
        EXECUTOR.submit(task);
    }

    private void loadCustomers() {
        loadingSpinner.setVisible(true);
        emptyState.setVisible(false);
        errorBox.setVisible(false);
        errorBox.setManaged(false);

        String query = searchField.getText().trim();
        String status = statusFilter.getValue();
        String type = typeFilter.getValue();

        Task<List<CustomerModels.CustomerRow>> task = new Task<>() {
            @Override
            protected List<CustomerModels.CustomerRow> call() throws Exception {
                return CustomerDao.search(query, status, type);
            }
        };

        task.setOnSucceeded(e -> {
            loadingSpinner.setVisible(false);
            List<CustomerModels.CustomerRow> rows = task.getValue();
            tableRows.setAll(rows);
            emptyState.setVisible(rows.isEmpty());
        });

        task.setOnFailed(e -> {
            loadingSpinner.setVisible(false);
            Throwable ex = task.getException();
            errorMsgLbl.setText("Query Error: " + (ex != null ? ex.getMessage() : "Unknown error"));
            errorBox.setVisible(true);
            errorBox.setManaged(true);
            ToastManager.showError("Failed to query customers: " + (ex != null ? ex.getMessage() : ""));
        });

        EXECUTOR.submit(task);
    }

    // --- CSV EXPORT WITH PROPER QUOTE ESCAPING ---
    private void exportFilteredCsv() {
        if (tableRows.isEmpty()) {
            ToastManager.showInfo("No records available to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Filtered Customers (CSV)");
        fileChooser.setInitialFileName("customers_export_" + System.currentTimeMillis() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Spreadsheet (*.csv)", "*.csv"));

        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file == null) return;

        try (PrintWriter pw = new PrintWriter(file)) {
            pw.println("\"Customer ID\",\"Subscriber Name\",\"Email Address\",\"Phone Number\",\"Customer Type\",\"Service Area\",\"Status\",\"Registration Date\"");
            for (CustomerModels.CustomerRow r : tableRows) {
                pw.println(String.join(",",
                    csvEscape(String.valueOf(r.customerId())),
                    csvEscape(r.name()),
                    csvEscape(r.email()),
                    csvEscape(r.phone()),
                    csvEscape(r.customerType()),
                    csvEscape(r.areaName()),
                    csvEscape(r.status()),
                    csvEscape(r.createdAt())
                ));
            }
            ToastManager.showSuccess("Exported " + tableRows.size() + " customers to " + file.getName());
        } catch (Exception e) {
            ToastManager.showError("Export failed: " + e.getMessage());
        }
    }

    private static String csvEscape(String val) {
        if (val == null) return "\"\"";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }

    // --- ADD / EDIT CUSTOMER DIALOG WITH ALL FIELDS AND INLINE RED ERRORS ---
    private void openCustomerFormDialog(Integer customerIdToEdit) {
        boolean isEdit = customerIdToEdit != null;

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Edit Subscriber #" + customerIdToEdit : "Register New Subscriber");
        dialog.setHeaderText(null);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node defaultBar = dialog.getDialogPane().lookup(".button-bar");
        if (defaultBar != null) {
            defaultBar.setVisible(false);
            defaultBar.setManaged(false);
        }

        dialog.getDialogPane().addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                dialog.setResult(false);
                dialog.close();
                event.consume();
            }
        });

        VBox content = new VBox(12);
        content.getStyleClass().add("card");
        content.setPrefWidth(540);

        Label titleLbl = new Label(isEdit ? "Update Subscriber Information" : "Customer Registration Form");
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-text;");

        // Type
        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("INDIVIDUAL", "BUSINESS");
        typeBox.setValue("INDIVIDUAL");
        typeBox.setMaxWidth(Double.MAX_VALUE);

        // Fields
        TextField fName = new TextField();
        FormField fNameField = new FormField("First Name", fName, true);

        TextField lName = new TextField();
        FormField lNameField = new FormField("Last Name", lName, true);

        TextField comp = new TextField();
        FormField compField = new FormField("Company Name", comp, false);

        TextField email = new TextField();
        FormField emailField = new FormField("Email Address", email, true);

        TextField phone = new TextField();
        phone.setPromptText("10-digit mobile number");
        FormField phoneField = new FormField("Phone Number (10 digits)", phone, true);

        TextField altPhone = new TextField();
        altPhone.setPromptText("Optional secondary contact");
        FormField altPhoneField = new FormField("Alternate Phone", altPhone, false);

        ComboBox<CustomerModels.ServiceAreaItem> areaBox = new ComboBox<>();
        areaBox.setMaxWidth(Double.MAX_VALUE);
        areaBox.getItems().setAll(cachedAreas);
        if (!cachedAreas.isEmpty()) areaBox.setValue(cachedAreas.get(0));

        VBox areaContainer = new VBox(4);
        Label areaLbl = new Label("Service Area *");
        areaLbl.getStyleClass().add("form-label");
        Label areaErr = new Label();
        areaErr.getStyleClass().add("form-error");
        areaErr.setVisible(false);
        areaErr.setManaged(false);
        areaContainer.getChildren().addAll(areaLbl, areaBox, areaErr);

        TextField idType = new TextField();
        idType.setPromptText("AADHAAR, PAN, PASSPORT, VOTER_ID");
        FormField idTypeField = new FormField("ID Proof Type", idType, false);

        TextField idNumber = new TextField();
        FormField idNumberField = new FormField("ID Proof Number", idNumber, false);

        TextField gstNumber = new TextField();
        gstNumber.setPromptText("GSTIN for Business");
        FormField gstField = new FormField("GST Number (Business accounts)", gstNumber, false);

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("ACTIVE", "INACTIVE", "BLACKLISTED");
        statusBox.setValue("ACTIVE");
        statusBox.setMaxWidth(Double.MAX_VALUE);

        VBox statusContainer = new VBox(4);
        Label statusLbl = new Label("Lifecycle Status *");
        statusLbl.getStyleClass().add("form-label");
        statusContainer.getChildren().addAll(statusLbl, statusBox);

        // Dynamic Company & GST requirement on Type switch
        typeBox.setOnAction(e -> {
            boolean isBiz = "BUSINESS".equals(typeBox.getValue());
            compField.getInput().setPromptText(isBiz ? "Required registered business name" : "Optional");
            gstField.getInput().setPromptText(isBiz ? "Required 15-digit GSTIN" : "Not applicable for individuals");
        });

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> {
            dialog.setResult(false);
            dialog.close();
        });

        Button saveBtn = new Button(isEdit ? "Update Subscriber" : "Register Account");
        saveBtn.getStyleClass().add("btn-primary");
        if (isEdit) {
            saveBtn.setDisable(true); // disabled until findById completes
        }
        btnRow.getChildren().addAll(cancelBtn, saveBtn);

        // Preload values if in Edit mode
        if (isEdit) {
            Task<CustomerModels.CustomerDetails> loadTask = new Task<>() {
                @Override
                protected CustomerModels.CustomerDetails call() throws Exception {
                    return CustomerDao.findById(customerIdToEdit);
                }
            };
            loadTask.setOnSucceeded(e -> {
                CustomerModels.CustomerDetails d = loadTask.getValue();
                if (d != null) {
                    typeBox.setValue(d.customerType());
                    fName.setText(d.firstName());
                    lName.setText(d.lastName());
                    comp.setText(d.companyName() != null ? d.companyName() : "");
                    email.setText(d.email());
                    phone.setText(d.phone());
                    altPhone.setText(d.alternatePhone() != null ? d.alternatePhone() : "");
                    idType.setText(d.idProofType() != null ? d.idProofType() : "");
                    idNumber.setText(d.idProofNumber() != null ? d.idProofNumber() : "");
                    gstNumber.setText(d.gstNumber() != null ? d.gstNumber() : "");
                    statusBox.setValue(d.status());
                    for (CustomerModels.ServiceAreaItem item : cachedAreas) {
                        if (item.areaId() == d.areaId()) {
                            areaBox.setValue(item);
                            break;
                        }
                    }
                }
                saveBtn.setDisable(false); // data loaded — allow saving
            });
            loadTask.setOnFailed(e -> {
                saveBtn.setDisable(false); // allow retry even on failure
                ToastManager.showError("Failed to load customer data for editing.");
            });
            EXECUTOR.submit(loadTask);
        }

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(500);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox formFields = new VBox(10);
        formFields.getChildren().addAll(
            titleLbl,
            new Label("Account Type *"), typeBox,
            new HBox(10, fNameField, lNameField),
            compField,
            new HBox(10, emailField, phoneField),
            new HBox(10, altPhoneField, areaContainer),
            new HBox(10, idTypeField, idNumberField),
            gstField,
            statusContainer,
            btnRow
        );
        scroll.setContent(formFields);
        content.getChildren().add(scroll);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/isp-theme.css").toExternalForm());

        // Save Action with full validation
        saveBtn.setOnAction(e -> {
            fNameField.clearError();
            lNameField.clearError();
            compField.clearError();
            emailField.clearError();
            phoneField.clearError();
            altPhoneField.clearError();
            gstField.clearError();
            areaErr.setVisible(false);
            areaErr.setManaged(false);

            String cType = typeBox.getValue();
            String fn = fName.getText().trim();
            String ln = lName.getText().trim();
            String cn = comp.getText().trim();
            String em = email.getText().trim();
            String ph = phone.getText().trim();
            String alt = altPhone.getText().trim();
            String idT = idType.getText().trim();
            String idN = idNumber.getText().trim();
            String gst = gstNumber.getText().trim();
            String st = statusBox.getValue();
            CustomerModels.ServiceAreaItem area = areaBox.getValue();

            boolean valid = true;

            if (fn.isEmpty()) { fNameField.setError("First name is required"); valid = false; }
            if (ln.isEmpty()) { lNameField.setError("Last name is required"); valid = false; }

            if ("BUSINESS".equals(cType)) {
                if (cn.isEmpty()) { compField.setError("Company name is required for Business accounts"); valid = false; }
                if (gst.isEmpty()) {
                    gstField.setError("GST number is required for Business accounts");
                    valid = false;
                } else if (gst.length() != 15) {
                    gstField.setError("GST number must be exactly 15 characters");
                    valid = false;
                }
            }

            if (!em.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                emailField.setError("Enter a valid email address");
                valid = false;
            }

            // Exactly 10 digits validation
            if (!ph.matches("^\\d{10}$")) {
                phoneField.setError("Phone must be exactly 10 digits");
                valid = false;
            }

            if (!alt.isEmpty() && !alt.matches("^\\d{10}$")) {
                altPhoneField.setError("Alternate phone must be 10 digits");
                valid = false;
            }

            if (area == null) {
                areaErr.setText("Please select a service area");
                areaErr.setVisible(true);
                areaErr.setManaged(true);
                valid = false;
            }

            if (!valid) return;

            saveBtn.setDisable(true);

            Task<Void> saveTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    // Pre-check duplicate email/phone
                    if (CustomerDao.existsByEmail(em, customerIdToEdit)) {
                        throw new IllegalArgumentException("DUPLICATE_EMAIL");
                    }
                    if (CustomerDao.existsByPhone(ph, customerIdToEdit)) {
                        throw new IllegalArgumentException("DUPLICATE_PHONE");
                    }

                    if (isEdit) {
                        CustomerDao.update(customerIdToEdit, cType, fn, ln, cn, em, ph, alt, area.areaId(), idT, idN, gst, st);
                    } else {
                        CustomerDao.insert(cType, fn, ln, cn, em, ph, alt, area.areaId(), idT, idN, gst, st);
                    }
                    return null;
                }
            };

            saveTask.setOnSucceeded(ev -> {
                dialog.setResult(true);
                dialog.close();
                ToastManager.showSuccess(isEdit ? "Customer updated successfully!" : "Customer registered successfully!");
                loadCustomers();
            });

            saveTask.setOnFailed(ev -> {
                saveBtn.setDisable(false);
                Throwable cause = saveTask.getException();
                if (cause instanceof IllegalArgumentException) {
                    if ("DUPLICATE_EMAIL".equals(cause.getMessage())) {
                        emailField.setError("This email address is already registered to another subscriber.");
                    } else if ("DUPLICATE_PHONE".equals(cause.getMessage())) {
                        phoneField.setError("This phone number is already registered to another subscriber.");
                    }
                } else if (cause != null && cause.getMessage() != null && cause.getMessage().contains("ORA-00001")) {
                    if (cause.getMessage().contains("UQ_CUSTOMER_EMAIL")) {
                        emailField.setError("Email already in use (ORA-00001).");
                    } else {
                        ToastManager.showError("Unique constraint violation: " + cause.getMessage());
                    }
                } else {
                    ToastManager.showError("Save error: " + (cause != null ? cause.getMessage() : "Unknown error"));
                }
            });

            EXECUTOR.submit(saveTask);
        });

        dialog.showAndWait();
    }

    // --- DEACTIVATE ACCOUNT ACTION WITH CONFIRMATION ---
    private void openDeactivateConfirm(CustomerModels.CustomerRow c) {
        ConfirmDialog confirm = new ConfirmDialog(
            "Deactivate Customer #" + c.customerId(),
            "Are you sure you want to mark subscriber \"" + c.name() + "\" as INACTIVE?\nExisting contracts and billing records remain preserved for audit compliance.",
            "Deactivate Account",
            true
        );

        confirm.showAndWait().ifPresent(confirmed -> {
            if (confirmed) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        CustomerDao.deactivate(c.customerId());
                        return null;
                    }
                };
                task.setOnSucceeded(ev -> {
                    ToastManager.showSuccess("Subscriber #" + c.customerId() + " deactivated.");
                    loadCustomers();
                });
                task.setOnFailed(ev -> ToastManager.showError("Failed to deactivate: " + task.getException().getMessage()));
                EXECUTOR.submit(task);
            }
        });
    }

    // --- 360-DEGREE SUBSCRIBER DETAIL MODAL (SUBSCRIPTIONS, INVOICES, TICKETS) ---
    private void openDetailsDialog(int customerId) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Subscriber Details #" + customerId);
        dialog.setHeaderText(null);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node defaultBar = dialog.getDialogPane().lookup(".button-bar");
        if (defaultBar != null) {
            defaultBar.setVisible(false);
            defaultBar.setManaged(false);
        }

        dialog.getDialogPane().addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                dialog.close();
                event.consume();
            }
        });

        VBox content = new VBox(14);
        content.getStyleClass().add("card");
        content.setPrefWidth(680);
        content.setPadding(new Insets(18));

        ProgressIndicator spin = new ProgressIndicator();
        spin.setMaxSize(36, 36);
        content.getChildren().add(spin);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/isp-theme.css").toExternalForm());

        Task<CustomerModels.CustomerDetails> task = new Task<>() {
            @Override
            protected CustomerModels.CustomerDetails call() throws Exception {
                return CustomerDao.findById(customerId);
            }
        };

        task.setOnSucceeded(e -> {
            content.getChildren().remove(spin);
            CustomerModels.CustomerDetails d = task.getValue();
            if (d == null) {
                content.getChildren().add(new Label("Customer record not found."));
                return;
            }

            // Top Header: Name, ID, Badges
            HBox top = new HBox(12);
            top.setAlignment(Pos.CENTER_LEFT);

            FontIcon uIcon = new FontIcon("fas-user-circle");
            uIcon.setIconSize(32);
            uIcon.setStyle("-fx-icon-color: -color-primary;");

            VBox nameBox = new VBox(2);
            Label nameLbl = new Label(d.firstName() + " " + d.lastName());
            nameLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -color-text;");
            Label idTag = new Label("Customer ID #" + d.customerId() + " • " + d.customerType() + " • Area: " + d.areaName());
            idTag.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-muted;");
            nameBox.getChildren().addAll(nameLbl, idTag);

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            StatusBadge badge = new StatusBadge(d.status());
            top.getChildren().addAll(uIcon, nameBox, sp, badge);

            // TabPane for Overview, Subscriptions, Invoices, Support Tickets
            TabPane tabPane = new TabPane();
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            tabPane.setPrefHeight(340);

            // Tab 1: Overview
            GridPane grid = new GridPane();
            grid.setHgap(16);
            grid.setVgap(10);
            grid.setPadding(new Insets(12));

            addDetailField(grid, 0, 0, "Email", d.email());
            addDetailField(grid, 1, 0, "Phone", d.phone());
            addDetailField(grid, 0, 1, "Alternate Phone", d.alternatePhone() != null ? d.alternatePhone() : "-");
            addDetailField(grid, 1, 1, "Company", d.companyName() != null ? d.companyName() : "-");
            addDetailField(grid, 0, 2, "ID Proof", (d.idProofType() != null ? d.idProofType() + ": " : "") + (d.idProofNumber() != null ? d.idProofNumber() : "-"));
            addDetailField(grid, 1, 2, "GSTIN", d.gstNumber() != null ? d.gstNumber() : "-");
            addDetailField(grid, 0, 3, "Account Created", d.createdAt());
            addDetailField(grid, 1, 3, "Last Modified", d.updatedAt() != null ? d.updatedAt() : "-");

            Tab tab1 = new Tab("Contact & Identity", grid);

            // Tab 2: Subscriptions
            VBox subBox = new VBox(8);
            subBox.setPadding(new Insets(10));
            if (d.subscriptions().isEmpty()) {
                subBox.getChildren().add(new Label("No broadband contracts found for this subscriber."));
            } else {
                for (CustomerModels.CustomerDetails.SubInfo s : d.subscriptions()) {
                    HBox sRow = new HBox(12);
                    sRow.setAlignment(Pos.CENTER_LEFT);
                    sRow.setStyle("-fx-padding: 8; -fx-background-color: -color-surface-hover; -fx-background-radius: 6;");
                    Label pLbl = new Label(s.planName() + " (" + s.planType() + ")");
                    pLbl.setStyle("-fx-font-weight: bold;");
                    Region sSpacer = new Region();
                    HBox.setHgrow(sSpacer, Priority.ALWAYS);
                    Label dates = new Label(s.startDate() + " to " + s.endDate());
                    dates.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-muted;");
                    StatusBadge sBadge = new StatusBadge(s.status());
                    sRow.getChildren().addAll(pLbl, dates, sSpacer, sBadge);
                    subBox.getChildren().add(sRow);
                }
            }
            Tab tab2 = new Tab("Subscriptions (" + d.subscriptions().size() + ")", new ScrollPane(subBox));

            // Tab 3: Recent Invoices
            VBox invBox = new VBox(8);
            invBox.setPadding(new Insets(10));
            if (d.lastInvoices().isEmpty()) {
                invBox.getChildren().add(new Label("No billing invoices issued yet."));
            } else {
                for (CustomerModels.CustomerDetails.InvInfo inv : d.lastInvoices()) {
                    HBox iRow = new HBox(12);
                    iRow.setAlignment(Pos.CENTER_LEFT);
                    iRow.setStyle("-fx-padding: 8; -fx-background-color: -color-surface-hover; -fx-background-radius: 6;");
                    Label iNum = new Label("Invoice #" + inv.invId() + " (Due " + inv.dueDate() + ")");
                    Label amt = new Label("Total: " + Formatters.formatCurrency(inv.totalAmount()) + " | Paid: " + Formatters.formatCurrency(inv.amountPaid()));
                    Region iSpacer = new Region();
                    HBox.setHgrow(iSpacer, Priority.ALWAYS);
                    StatusBadge iBadge = new StatusBadge(inv.status());
                    iRow.getChildren().addAll(iNum, amt, iSpacer, iBadge);
                    invBox.getChildren().add(iRow);
                }
            }
            Tab tab3 = new Tab("Recent Invoices (" + d.lastInvoices().size() + ")", new ScrollPane(invBox));

            // Tab 4: Support Tickets
            VBox tktBox = new VBox(8);
            tktBox.setPadding(new Insets(10));
            if (d.tickets().isEmpty()) {
                tktBox.getChildren().add(new Label("No support inquiries or incident tickets logged."));
            } else {
                for (CustomerModels.CustomerDetails.TicketInfo tk : d.tickets()) {
                    HBox tRow = new HBox(12);
                    tRow.setAlignment(Pos.CENTER_LEFT);
                    tRow.setStyle("-fx-padding: 8; -fx-background-color: -color-surface-hover; -fx-background-radius: 6;");
                    Label tSub = new Label("#" + tk.ticketId() + ": " + tk.subject());
                    Label prio = new Label("[" + tk.priority() + "]");
                    prio.setStyle("-fx-font-weight: 600; -fx-font-size: 11px;");
                    Region tSpacer = new Region();
                    HBox.setHgrow(tSpacer, Priority.ALWAYS);
                    StatusBadge tBadge = new StatusBadge(tk.status());
                    tRow.getChildren().addAll(tSub, prio, tSpacer, tBadge);
                    tktBox.getChildren().add(tRow);
                }
            }
            Tab tab4 = new Tab("Tickets (" + d.tickets().size() + ")", new ScrollPane(tktBox));

            tabPane.getTabs().addAll(tab1, tab2, tab3, tab4);

            Button closeBtn = new Button("Close View");
            closeBtn.getStyleClass().add("btn-secondary");
            closeBtn.setOnAction(ev -> dialog.close());

            HBox bot = new HBox(closeBtn);
            bot.setAlignment(Pos.CENTER_RIGHT);

            content.getChildren().addAll(top, tabPane, bot);
        });

        task.setOnFailed(e -> {
            content.getChildren().remove(spin);
            content.getChildren().add(new Label("Failed to load details: " + task.getException().getMessage()));
        });

        EXECUTOR.submit(task);
        dialog.showAndWait();
    }

    private void addDetailField(GridPane grid, int col, int row, String label, String val) {
        VBox b = new VBox(2);
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: -color-muted;");
        Label v = new Label(val);
        v.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text;");
        b.getChildren().addAll(l, v);
        grid.add(b, col, row);
    }
}
