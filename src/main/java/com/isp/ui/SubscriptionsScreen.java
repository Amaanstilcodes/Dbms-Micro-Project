package com.isp.ui;

import com.isp.Session;
import com.isp.dao.SubscriptionDao;
import com.isp.dao.SubscriptionDao.AreaItem;
import com.isp.dao.SubscriptionDao.CustomerItem;
import com.isp.dao.SubscriptionDao.PlanItem;
import com.isp.dao.SubscriptionDao.SubscriptionRow;
import com.isp.ui.components.ConfirmDialog;
import com.isp.ui.components.EmptyState;
import com.isp.ui.components.StatusBadge;
import com.isp.ui.components.StepIndicator;
import com.isp.ui.components.ToastManager;
import com.isp.ui.util.Formatters;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Subscriptions screen – shows all subscriptions across all statuses.
 *
 * Role gates (enforced here AND in SubscriptionDao):
 *   ADMIN         – full access: New, Suspend, Resume, Terminate
 *   BILLING_CLERK – New, Suspend, Resume  (Terminate hidden)
 *   SUPPORT_AGENT – read-only
 *   TECHNICIAN    – read-only
 */
public class SubscriptionsScreen extends VBox {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Subscriptions-Worker");
        t.setDaemon(true);
        return t;
    });

    private final TextField searchField;
    private final ComboBox<String> statusFilter;
    private final Button newBtn;
    private final Button suspendBtn;
    private final Button resumeBtn;
    private final Button terminateBtn;
    private final TableView<SubscriptionRow> tableView;
    private final ObservableList<SubscriptionRow> tableRows = FXCollections.observableArrayList();
    private final StackPane tableContainer;
    private final ProgressIndicator loadingSpinner;
    private final EmptyState emptyState;
    private final VBox errorBox;
    private final Label errorMsgLbl;
    private SubscriptionRow selected = null;

    public SubscriptionsScreen() {
        setSpacing(16);
        setPadding(new Insets(24));
        setFillWidth(true);

        String role = Session.getRole();
        if (role == null) role = "SUPPORT_AGENT";
        boolean isAdmin  = "ADMIN".equals(role);
        boolean canWrite = isAdmin || "BILLING_CLERK".equals(role);

        // ── Header ────────────────────────────────────────────────────────────
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(2);
        Label title = new Label("Subscriptions");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Manage broadband service plans for all subscribers");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        newBtn = new Button("New Subscription");
        newBtn.getStyleClass().add("btn-primary");
        FontIcon addIcon = new FontIcon("fas-plus");
        addIcon.setIconSize(13);
        newBtn.setGraphic(addIcon);
        newBtn.setVisible(canWrite);
        newBtn.setManaged(canWrite);
        newBtn.setOnAction(e -> openNewSubscriptionWizard());
        header.getChildren().addAll(titleBox, hSpacer, newBtn);

        // ── Filter bar ────────────────────────────────────────────────────────
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("filter-bar");
        FontIcon searchIcon = new FontIcon("fas-search");
        searchIcon.setIconSize(14);
        searchField = new TextField();
        searchField.setPromptText("Search by customer or plan\u2026");
        searchField.setPrefWidth(280);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All", "ACTIVE", "PENDING", "SUSPENDED", "TERMINATED");
        statusFilter.setValue("All");
        statusFilter.setPrefWidth(160);

        suspendBtn = new Button("Suspend");
        suspendBtn.getStyleClass().add("btn-warning");
        FontIcon si = new FontIcon("fas-pause-circle"); si.setIconSize(13);
        suspendBtn.setGraphic(si);
        suspendBtn.setDisable(true);
        suspendBtn.setVisible(canWrite); suspendBtn.setManaged(canWrite);
        suspendBtn.setOnAction(e -> changeStatusAction("SUSPENDED",
                "Suspend subscription?",
                "The customer will lose internet access until the subscription is resumed.", true));

        resumeBtn = new Button("Resume");
        resumeBtn.getStyleClass().add("btn-success");
        FontIcon ri = new FontIcon("fas-play-circle"); ri.setIconSize(13);
        resumeBtn.setGraphic(ri);
        resumeBtn.setDisable(true);
        resumeBtn.setVisible(canWrite); resumeBtn.setManaged(canWrite);
        resumeBtn.setOnAction(e -> changeStatusAction("ACTIVE",
                "Resume subscription?",
                "This will reactivate the subscription and restore internet access.", false));

        terminateBtn = new Button("Terminate");
        terminateBtn.getStyleClass().add("btn-danger");
        FontIcon ti = new FontIcon("fas-ban"); ti.setIconSize(13);
        terminateBtn.setGraphic(ti);
        terminateBtn.setDisable(true);
        terminateBtn.setVisible(isAdmin); terminateBtn.setManaged(isAdmin);
        terminateBtn.setOnAction(e -> changeStatusAction("TERMINATED",
                "Terminate subscription?",
                "This permanently terminates the subscription. It cannot be undone.", true));

        Region fbSpacer = new Region();
        filterBar.getChildren().addAll(searchIcon, searchField, statusFilter,
                fbSpacer, suspendBtn, resumeBtn, terminateBtn);
        HBox.setHgrow(fbSpacer, Priority.ALWAYS);

        // ── Table ─────────────────────────────────────────────────────────────
        tableView = new TableView<>(tableRows);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.getStyleClass().add("isp-table");
        tableView.setPlaceholder(new Label(""));
        VBox.setVgrow(tableView, Priority.ALWAYS);

        TableColumn<SubscriptionRow, Integer> idCol = new TableColumn<>("#ID");
        idCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().subscriptionId()));
        idCol.setPrefWidth(60); idCol.setMaxWidth(70);

        TableColumn<SubscriptionRow, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().customerName()));
        customerCol.setPrefWidth(180);

        TableColumn<SubscriptionRow, String> planCol = new TableColumn<>("Plan");
        planCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().planName()));
        planCol.setPrefWidth(160);

        TableColumn<SubscriptionRow, String> priceCol = new TableColumn<>("Monthly");
        priceCol.setCellValueFactory(c ->
                new SimpleStringProperty(Formatters.formatCurrency(c.getValue().price())));
        priceCol.setPrefWidth(110);

        TableColumn<SubscriptionRow, String> areaCol = new TableColumn<>("Service Area");
        areaCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().areaName()));
        areaCol.setPrefWidth(150);

        TableColumn<SubscriptionRow, String> startCol = new TableColumn<>("Start Date");
        startCol.setCellValueFactory(c ->
                new SimpleStringProperty(Formatters.formatDate(c.getValue().startDate())));
        startCol.setPrefWidth(110);

        TableColumn<SubscriptionRow, StatusBadge> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c ->
                new SimpleObjectProperty<>(new StatusBadge(c.getValue().status())));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(StatusBadge badge, boolean empty) {
                super.updateItem(badge, empty);
                setGraphic(empty ? null : badge);
            }
        });
        statusCol.setPrefWidth(120);

        //noinspection unchecked
        tableView.getColumns().addAll(idCol, customerCol, planCol, priceCol, areaCol, startCol, statusCol);
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
            selected = row;
            updateActionButtons();
        });
        tableView.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) tableView.getSelectionModel().clearSelection();
        });

        // ── Overlays ──────────────────────────────────────────────────────────
        loadingSpinner = new ProgressIndicator();
        loadingSpinner.setPrefSize(48, 48);
        loadingSpinner.setVisible(false);

        emptyState = new EmptyState("No Subscriptions Found",
                "Try adjusting your search or filter criteria.", "fas-inbox");
        emptyState.setVisible(false);

        errorMsgLbl = new Label();
        errorMsgLbl.setWrapText(true);
        errorMsgLbl.getStyleClass().add("page-subtitle");

        Button retryBtn = new Button("Retry");
        retryBtn.getStyleClass().add("btn-secondary");
        FontIcon retryIcon = new FontIcon("fas-redo"); retryIcon.setIconSize(13);
        retryBtn.setGraphic(retryIcon);
        retryBtn.setOnAction(e -> refreshTable());

        FontIcon errIcon = new FontIcon("fas-exclamation-circle");
        errIcon.setIconSize(36);
        errIcon.setStyle("-fx-icon-color: -color-danger;");
        errorBox = new VBox(10, errIcon, errorMsgLbl, retryBtn);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setVisible(false);

        tableContainer = new StackPane(tableView, loadingSpinner, emptyState, errorBox);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        getChildren().addAll(header, filterBar, tableContainer);

        // ── Listeners ─────────────────────────────────────────────────────────
        PauseTransition debounce = new PauseTransition(Duration.millis(350));
        searchField.textProperty().addListener((obs, old, val) -> {
            debounce.setOnFinished(e -> refreshTable());
            debounce.playFromStart();
        });
        statusFilter.valueProperty().addListener((obs, old, val) -> refreshTable());

        refreshTable();
    }

    // ── Button state ──────────────────────────────────────────────────────────
    private void updateActionButtons() {
        if (selected == null) {
            suspendBtn.setDisable(true);
            resumeBtn.setDisable(true);
            terminateBtn.setDisable(true);
            return;
        }
        String s = selected.status();
        suspendBtn.setDisable(!"ACTIVE".equals(s));
        resumeBtn.setDisable(!("PENDING".equals(s) || "SUSPENDED".equals(s)));
        terminateBtn.setDisable(!("ACTIVE".equals(s) || "SUSPENDED".equals(s)));
    }

    // ── Async refresh ─────────────────────────────────────────────────────────
    private void refreshTable() {
        String text   = searchField.getText();
        String status = statusFilter.getValue();
        showLoading(true);

        Task<List<SubscriptionRow>> task = new Task<>() {
            @Override protected List<SubscriptionRow> call() throws Exception {
                return SubscriptionDao.search(text, status);
            }
        };
        task.setOnSucceeded(e -> {
            showLoading(false);
            List<SubscriptionRow> rows = task.getValue();
            tableRows.setAll(rows);
            tableView.setVisible(!rows.isEmpty());
            emptyState.setVisible(rows.isEmpty());
            errorBox.setVisible(false);
            selected = null;
            updateActionButtons();
        });
        task.setOnFailed(e -> {
            showLoading(false);
            tableView.setVisible(false);
            emptyState.setVisible(false);
            errorBox.setVisible(true);
            errorMsgLbl.setText("Failed to load subscriptions: " + task.getException().getMessage());
        });
        EXECUTOR.submit(task);
    }

    private void showLoading(boolean loading) {
        loadingSpinner.setVisible(loading);
        if (loading) {
            tableView.setVisible(false);
            emptyState.setVisible(false);
            errorBox.setVisible(false);
        }
    }

    // ── Status change ─────────────────────────────────────────────────────────
    private void changeStatusAction(String newStatus, String title, String message, boolean danger) {
        if (selected == null) return;
        ConfirmDialog dlg = new ConfirmDialog(title, message,
                "SUSPENDED".equals(newStatus) ? "Suspend" :
                "ACTIVE".equals(newStatus)    ? "Resume"  : "Terminate", danger);
        dlg.showAndWait().ifPresent(confirmed -> {
            if (!confirmed) return;
            int subId = selected.subscriptionId();
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    SubscriptionDao.changeStatus(subId, newStatus);
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                ToastManager.showSuccess("Subscription #" + subId + " \u2192 " + newStatus + ".");
                refreshTable();
            });
            task.setOnFailed(e -> {
                Throwable ex = task.getException();
                // IllegalStateException carries friendly ORA-20xxx or "not found" messages
                String msg = ex instanceof SecurityException
                        ? "Permission denied: " + ex.getMessage()
                        : ex.getMessage();
                ToastManager.showError(msg != null ? msg : "Unknown error.");
            });
            EXECUTOR.submit(task);
        });
    }

    // ── 3-Step New Subscription wizard ────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void openNewSubscriptionWizard() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("New Subscription");
        dialog.setHeaderText(null);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node defaultBar = dialog.getDialogPane().lookup(".button-bar");
        if (defaultBar != null) { defaultBar.setVisible(false); defaultBar.setManaged(false); }

        dialog.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) { dialog.setResult(false); dialog.close(); }
        });

        final int[]          currentStep      = {1};
        final CustomerItem[] selectedCustomer = {null};
        final PlanItem[]     selectedPlan     = {null};
        final AreaItem[]     selectedArea     = {null};

        StepIndicator steps = new StepIndicator("Select Customer", "Choose Plan & Area", "Confirm");

        // Step 1
        ComboBox<CustomerItem> customerBox = new ComboBox<>();
        customerBox.setMaxWidth(Double.MAX_VALUE);
        customerBox.setPromptText("Loading\u2026");
        VBox step1 = new VBox(10,
                styledLabel("Step 1 \u2013 Select Customer"),
                new Label("Active Customer:"), customerBox);

        // Step 2
        ComboBox<PlanItem> planBox = new ComboBox<>();
        planBox.setMaxWidth(Double.MAX_VALUE);
        planBox.setPromptText("Loading\u2026");
        ComboBox<AreaItem> areaBox = new ComboBox<>();
        areaBox.setMaxWidth(Double.MAX_VALUE);
        areaBox.setPromptText("Loading\u2026");
        VBox step2 = new VBox(10,
                styledLabel("Step 2 \u2013 Choose Plan & Service Area"),
                new Label("Plan:"), planBox,
                new Label("Service Area:"), areaBox);

        // Step 3
        Label sumCustomer = new Label(), sumPlan = new Label(),
              sumArea = new Label(), sumGst = new Label(), sumTotal = new Label();
        sumTotal.setStyle("-fx-font-weight:bold;");
        VBox summaryCard = new VBox(6, sumCustomer, sumPlan, sumArea,
                new Separator(), sumGst, sumTotal);
        summaryCard.getStyleClass().add("card");
        summaryCard.setPadding(new Insets(12));
        VBox step3 = new VBox(10, styledLabel("Step 3 \u2013 Confirm"), summaryCard);

        StackPane stepContent = new StackPane(step1);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> { dialog.setResult(false); dialog.close(); });

        Button backBtn = new Button("\u2190 Back");
        backBtn.getStyleClass().add("btn-secondary");
        backBtn.setDisable(true);

        Button nextBtn = new Button("Next Step \u2192");
        nextBtn.getStyleClass().add("btn-primary");

        HBox navRow = new HBox(10, cancelBtn, new Region(), backBtn, nextBtn);
        navRow.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(navRow.getChildren().get(1), Priority.ALWAYS);
        navRow.setPadding(new Insets(12, 0, 4, 0));

        VBox.setVgrow(stepContent, Priority.ALWAYS);

        VBox inner = new VBox(0, steps, stepContent, new Separator(), navRow);
        inner.setPadding(new Insets(20));
        inner.setPrefWidth(520);
        inner.setPrefHeight(460);
        dialog.getDialogPane().setContent(inner);
        dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/isp-theme.css").toExternalForm());

        // Async load dropdowns
        Task<Void> loadTask = new Task<>() {
            List<CustomerItem> customers;
            List<PlanItem>     plans;
            List<AreaItem>     areas;
            @Override protected Void call() throws Exception {
                customers = SubscriptionDao.loadActiveCustomers();
                plans     = SubscriptionDao.loadActivePlans();
                areas     = SubscriptionDao.loadAreas();
                return null;
            }
            @Override protected void succeeded() {
                Platform.runLater(() -> {
                    customerBox.getItems().setAll(customers);
                    planBox.getItems().setAll(plans);
                    areaBox.getItems().setAll(areas);
                    customerBox.setPromptText("Select customer\u2026");
                    planBox.setPromptText("Select plan\u2026");
                    areaBox.setPromptText("Select area\u2026");
                    if (!customers.isEmpty()) customerBox.setValue(customers.get(0));
                    if (!plans.isEmpty())     planBox.setValue(plans.get(0));
                    if (!areas.isEmpty())     areaBox.setValue(areas.get(0));
                });
            }
            @Override protected void failed() {
                Platform.runLater(() ->
                    ToastManager.showError("Failed to load wizard data: " + getException().getMessage()));
            }
        };
        EXECUTOR.submit(loadTask);

        backBtn.setOnAction(e -> {
            currentStep[0]--;
            steps.setStep(currentStep[0]);
            if (currentStep[0] == 1) {
                stepContent.getChildren().setAll(step1);
                backBtn.setDisable(true);
                nextBtn.setText("Next Step \u2192");
            } else if (currentStep[0] == 2) {
                stepContent.getChildren().setAll(step2);
                nextBtn.setText("Next Step \u2192");
            }
        });

        nextBtn.setOnAction(e -> {
            if (currentStep[0] == 1) {
                if (customerBox.getValue() == null) {
                    ToastManager.showError("Please select a customer.");
                    return;
                }
                selectedCustomer[0] = customerBox.getValue();
                currentStep[0] = 2;
                steps.setStep(2);
                stepContent.getChildren().setAll(step2);
                backBtn.setDisable(false);

            } else if (currentStep[0] == 2) {
                if (planBox.getValue() == null || areaBox.getValue() == null) {
                    ToastManager.showError("Please select both a plan and a service area.");
                    return;
                }
                selectedPlan[0] = planBox.getValue();
                selectedArea[0] = areaBox.getValue();
                double base = selectedPlan[0].price();
                double gst  = Math.round(base * SubscriptionDao.GST_RATE * 100.0) / 100.0;
                sumCustomer.setText("Subscriber: " + selectedCustomer[0].display());
                sumPlan.setText("Plan: " + selectedPlan[0].name()
                        + " (" + selectedPlan[0].speedMbps() + " Mbps @ "
                        + Formatters.formatCurrency(base) + "/mo)");
                sumArea.setText("Service Area: " + selectedArea[0].display());
                sumGst.setText("GST (18%): " + Formatters.formatCurrency(gst));
                sumTotal.setText("Total First Month: " + Formatters.formatCurrency(base + gst));
                currentStep[0] = 3;
                steps.setStep(3);
                stepContent.getChildren().setAll(step3);
                nextBtn.setText("Confirm & Provision");

            } else {
                nextBtn.setDisable(true);
                backBtn.setDisable(true);
                int custId = selectedCustomer[0].id();
                int planId = selectedPlan[0].id();
                int areaId = selectedArea[0].id();
                Task<Void> createTask = new Task<>() {
                    @Override protected Void call() throws Exception {
                        SubscriptionDao.create(custId, planId, areaId);
                        return null;
                    }
                };
                createTask.setOnSucceeded(ev -> {
                    dialog.setResult(true);
                    dialog.close();
                    ToastManager.showSuccess("Subscription provisioned successfully.");
                    refreshTable();
                });
                createTask.setOnFailed(ev -> {
                    nextBtn.setDisable(false);
                    backBtn.setDisable(false);
                    Throwable ex = createTask.getException();
                    String msg = (ex instanceof IllegalStateException || ex instanceof SecurityException)
                            ? ex.getMessage()
                            : "Provisioning failed: " + ex.getMessage();
                    ToastManager.showError(msg);
                });
                EXECUTOR.submit(createTask);
            }
        });

        dialog.showAndWait();
    }

    private static Label styledLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight:bold; -fx-font-size:14;");
        return l;
    }
}
