package com.isp.ui.components;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Reusable modal dialog wrapper providing:
 * - Proper window titlebar close button (X)
 * - ESC key handling
 * - Clean custom bottom action bar with Cancel button
 * - CSS stylesheet injection
 */
public class AppDialog extends Dialog<Boolean> {
    private final VBox rootLayout;
    private final HBox buttonBar;
    private final Button cancelBtn;

    public AppDialog(String title, double width) {
        setTitle(title);
        setHeaderText(null);

        // Required by JavaFX for close (X) and dialog.close() to work properly
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node defaultBar = getDialogPane().lookup(".button-bar");
        if (defaultBar != null) {
            defaultBar.setVisible(false);
            defaultBar.setManaged(false);
        }

        rootLayout = new VBox(16);
        rootLayout.getStyleClass().add("card");
        if (width > 0) {
            rootLayout.setPrefWidth(width);
        }

        buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> closeDialog(false));

        buttonBar.getChildren().addAll(spacer, cancelBtn);

        rootLayout.getChildren().add(buttonBar);
        getDialogPane().setContent(rootLayout);

        var css = getClass().getResource("/css/isp-theme.css");
        if (css != null) {
            getDialogPane().getStylesheets().add(css.toExternalForm());
        }

        // Close on ESC key
        getDialogPane().addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                closeDialog(false);
                event.consume();
            }
        });
    }

    public void setBody(Node... nodes) {
        // Keep buttonBar as the last child
        rootLayout.getChildren().clear();
        rootLayout.getChildren().addAll(nodes);
        rootLayout.getChildren().add(buttonBar);
    }

    public void addActionButton(Button button) {
        buttonBar.getChildren().add(button);
    }

    public Button getCancelButton() {
        return cancelBtn;
    }

    public void closeDialog(boolean success) {
        setResult(success);
        close();
    }
}
