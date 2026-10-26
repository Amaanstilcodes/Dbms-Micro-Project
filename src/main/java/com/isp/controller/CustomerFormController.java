package com.isp.controller;

import com.isp.model.Customer;
import com.isp.service.MockDataService;
import com.isp.service.MockDataService.AreaOption;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.function.Consumer;

/**
 * Controller for CustomerForm.fxml.
 *
 * <p>Works in two modes:
 * <ul>
 *   <li><strong>ADD mode</strong>  — call {@link #initAdd(Consumer)} before showing the stage.</li>
 *   <li><strong>EDIT mode</strong> — call {@link #initEdit(Customer, Consumer)} before showing the stage.</li>
 * </ul>
 * The {@code onSave} callback is fired with the saved {@link Customer} after a successful
 * save so the parent controller can refresh its table without coupling to this form.
 *
 * <p><strong>Phase B integration:</strong> replace the two
 * {@code MockDataService.insertCustomer / updateCustomer} calls with
 * {@code DAOFactory.getCustomerDAO().insert(c)} and {@code .update(c)} respectively.
 * All validation code stays unchanged.
 */
public class CustomerFormController {

    // ── Header ──────────────────────────────────────────────────────────
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;

    // ── Fields ──────────────────────────────────────────────────────────
    @FXML private ComboBox<String>     typeCombo;
    @FXML private ComboBox<String>     statusCombo;
    @FXML private TextField            firstNameField;
    @FXML private TextField            lastNameField;
    @FXML private VBox                 companyRow;
    @FXML private TextField            companyField;
    @FXML private TextField            emailField;
    @FXML private TextField            phoneField;
    @FXML private TextField            altPhoneField;
    @FXML private ComboBox<AreaOption> areaCombo;
    @FXML private ComboBox<String>     idTypeCombo;
    @FXML private TextField            idNumberField;
    @FXML private VBox                 gstRow;
    @FXML private TextField            gstField;

    // ── Error labels (one per field) ──────────────────────────────────
    @FXML private Label typeError;
    @FXML private Label statusError;
    @FXML private Label firstNameError;
    @FXML private Label lastNameError;
    @FXML private Label companyError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label altPhoneError;
    @FXML private Label areaError;
    @FXML private Label idTypeError;
    @FXML private Label idNumberError;
    @FXML private Label formErrorLabel;

    // ── Footer ──────────────────────────────────────────────────────────
    @FXML private Button saveButton;

    // ── Internal state ──────────────────────────────────────────────────
    private Customer       editTarget;        // null = ADD mode
    private Consumer<Customer> onSave;        // callback to parent controller

    // ────────────────────────────────────────────────────────────────────
    // JavaFX lifecycle
    // ────────────────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        typeCombo.setItems(FXCollections.observableArrayList("INDIVIDUAL", "BUSINESS"));
        typeCombo.setValue("INDIVIDUAL");

        statusCombo.setItems(FXCollections.observableArrayList("ACTIVE", "INACTIVE", "BLACKLISTED"));
        statusCombo.setValue("ACTIVE");

        idTypeCombo.setItems(FXCollections.observableArrayList("AADHAAR", "PAN", "PASSPORT", "VOTER_ID", "DRIVING_LICENCE"));
        idTypeCombo.setValue("AADHAAR");

        // Populate areas from MockDataService
        // Phase B: replace with DAOFactory.getServiceAreaDAO().findAll()
        areaCombo.setItems(FXCollections.observableArrayList(MockDataService.getServiceAreas()));

        // Show/hide company + GST fields based on type selection
        typeCombo.valueProperty().addListener((obs, oldVal, newVal) -> toggleBusinessFields(newVal));
        toggleBusinessFields("INDIVIDUAL"); // initial state

        // Clear field error when user starts typing / selecting
        wireErrorClear(firstNameField, firstNameError);
        wireErrorClear(lastNameField,  lastNameError);
        wireErrorClear(emailField,     emailError);
        wireErrorClear(phoneField,     phoneError);
        wireErrorClear(altPhoneField,  altPhoneError);
        wireErrorClear(idNumberField,  idNumberError);
        wireErrorClear(companyField,   companyError);
        typeCombo.valueProperty().addListener((o, p, n) -> hide(typeError));
        statusCombo.valueProperty().addListener((o, p, n) -> hide(statusError));
        areaCombo.valueProperty().addListener((o, p, n)  -> hide(areaError));
        idTypeCombo.valueProperty().addListener((o, p, n)-> hide(idTypeError));
    }

    // ────────────────────────────────────────────────────────────────────
    // Public init methods called by CustomersController
    // ────────────────────────────────────────────────────────────────────

    /** Configures the form for ADD mode. Must be called before the stage is shown. */
    public void initAdd(Consumer<Customer> onSaveCallback) {
        this.editTarget = null;
        this.onSave     = onSaveCallback;
        titleLabel.setText("Add Customer");
        subtitleLabel.setText("Fill in all required fields and click Save.");
        // Status combo hidden in ADD — new customers always start as ACTIVE
        statusCombo.setValue("ACTIVE");
        statusCombo.setDisable(true);
    }

    /** Configures the form for EDIT mode and pre-fills all fields. Must be called before the stage is shown. */
    public void initEdit(Customer customer, Consumer<Customer> onSaveCallback) {
        this.editTarget = customer;
        this.onSave     = onSaveCallback;
        titleLabel.setText("Edit Customer — #" + customer.getCustomerId());
        subtitleLabel.setText("Update the fields you want to change, then click Save.");
        statusCombo.setDisable(false);

        // Pre-fill every field from the existing customer object
        typeCombo.setValue(customer.getCustomerType());
        statusCombo.setValue(customer.getStatus());
        firstNameField.setText(customer.getFirstName());
        lastNameField.setText(customer.getLastName());
        companyField.setText(nullToEmpty(customer.getCompanyName()));
        emailField.setText(customer.getEmail());
        phoneField.setText(customer.getPhone());
        altPhoneField.setText(nullToEmpty(customer.getAlternatePhone()));
        idTypeCombo.setValue(customer.getIdProofType());
        idNumberField.setText(customer.getIdProofNumber());
        gstField.setText(nullToEmpty(customer.getGstNumber()));

        // Select matching area in combo
        MockDataService.getServiceAreas().stream()
                .filter(a -> a.areaId() == customer.getAreaId())
                .findFirst()
                .ifPresent(areaCombo::setValue);

        toggleBusinessFields(customer.getCustomerType());
    }

    // ────────────────────────────────────────────────────────────────────
    // Button handlers
    // ────────────────────────────────────────────────────────────────────

    @FXML
    private void handleSave() {
        hideFormError();
        if (!validateAll()) return;   // validation failed — errors already shown inline

        Customer c = buildCustomerFromForm();

        try {
            if (editTarget == null) {
                // ── ADD ──────────────────────────────────────────────────
                // Phase A: MockDataService
                // Phase B: int id = DAOFactory.getCustomerDAO().insert(c);
                MockDataService.insertCustomer(c);
            } else {
                // ── EDIT ─────────────────────────────────────────────────
                c.setCustomerId(editTarget.getCustomerId());
                c.setCreatedAt(editTarget.getCreatedAt());
                // Phase A: MockDataService
                // Phase B: DAOFactory.getCustomerDAO().update(c);
                MockDataService.updateCustomer(c);
            }

            if (onSave != null) onSave.accept(c);  // signal parent to refresh
            closeStage();

        } catch (Exception ex) {
            // Show the error from mock (simulates DB trigger / constraint)
            showFormError(ex.getMessage() != null ? ex.getMessage()
                          : "An unexpected error occurred. Please try again.");
        }
    }

    @FXML
    private void handleCancel() {
        closeStage();
    }

    // ────────────────────────────────────────────────────────────────────
    // Validation
    // ────────────────────────────────────────────────────────────────────

    /** Returns true only when every field passes. Shows inline error labels on failures. */
    private boolean validateAll() {
        boolean ok = true;

        // Customer type
        if (typeCombo.getValue() == null) {
            show(typeError, "Select a customer type.");
            ok = false;
        }

        // First name
        if (firstNameField.getText().isBlank()) {
            show(firstNameError, "First name is required.");
            ok = false;
        } else if (firstNameField.getText().strip().length() < 2) {
            show(firstNameError, "First name must be at least 2 characters.");
            ok = false;
        }

        // Last name
        if (lastNameField.getText().isBlank()) {
            show(lastNameError, "Last name is required.");
            ok = false;
        }

        // Company name (BUSINESS only)
        if ("BUSINESS".equals(typeCombo.getValue()) && companyField.getText().isBlank()) {
            show(companyError, "Company name is required for Business customers.");
            ok = false;
        }

        // Email
        String email = emailField.getText().strip();
        if (email.isEmpty()) {
            show(emailError, "Email address is required.");
            ok = false;
        } else if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")) {
            show(emailError, "Enter a valid email address (e.g. name@domain.com).");
            ok = false;
        }

        // Phone — exactly 10 digits
        String phone = phoneField.getText().strip();
        if (phone.isEmpty()) {
            show(phoneError, "Phone number is required.");
            ok = false;
        } else if (!phone.matches("\\d{10}")) {
            show(phoneError, "Phone must be exactly 10 digits (no spaces or dashes).");
            ok = false;
        }

        // Alternate phone — optional but must be 10 digits if provided
        String altPhone = altPhoneField.getText().strip();
        if (!altPhone.isEmpty() && !altPhone.matches("\\d{10}")) {
            show(altPhoneError, "Alternate phone must be exactly 10 digits.");
            ok = false;
        }

        // Service area
        if (areaCombo.getValue() == null) {
            show(areaError, "Select a service area.");
            ok = false;
        }

        // ID proof type
        if (idTypeCombo.getValue() == null) {
            show(idTypeError, "Select an ID proof type.");
            ok = false;
        }

        // ID proof number
        if (idNumberField.getText().isBlank()) {
            show(idNumberError, "ID proof number is required.");
            ok = false;
        }

        return ok;
    }

    // ────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────

    /** Builds a Customer from the current form state (no ID set for ADD). */
    private Customer buildCustomerFromForm() {
        Customer c = new Customer();
        c.setCustomerType(typeCombo.getValue());
        c.setStatus(statusCombo.getValue());
        c.setFirstName(firstNameField.getText().strip());
        c.setLastName(lastNameField.getText().strip());
        c.setCompanyName(emptyToNull(companyField.getText()));
        c.setEmail(emailField.getText().strip().toLowerCase());
        c.setPhone(phoneField.getText().strip());
        c.setAlternatePhone(emptyToNull(altPhoneField.getText()));

        AreaOption area = areaCombo.getValue();
        if (area != null) {
            c.setAreaId(area.areaId());
            c.setAreaName(area.displayName());
        }

        c.setIdProofType(idTypeCombo.getValue());
        c.setIdProofNumber(idNumberField.getText().strip());
        c.setGstNumber(emptyToNull(gstField.getText()));
        return c;
    }

    private void toggleBusinessFields(String type) {
        boolean isBusiness = "BUSINESS".equals(type);
        companyRow.setVisible(isBusiness);
        companyRow.setManaged(isBusiness);
        gstRow.setVisible(isBusiness);
        gstRow.setManaged(isBusiness);
    }

    private void wireErrorClear(TextField field, Label errorLabel) {
        field.textProperty().addListener((obs, o, n) -> hide(errorLabel));
    }

    private void show(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hide(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }

    private void showFormError(String message) {
        formErrorLabel.setText(message);
        formErrorLabel.setVisible(true);
        formErrorLabel.setManaged(true);
    }

    private void hideFormError() {
        formErrorLabel.setVisible(false);
        formErrorLabel.setManaged(false);
    }

    private void closeStage() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
    private static String emptyToNull(String s)  { return (s == null || s.isBlank()) ? null : s.strip(); }
}
