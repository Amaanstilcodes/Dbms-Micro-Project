package com.isp.ui.components;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class FormField extends VBox {
    private final Label labelNode;
    private final TextField inputNode;
    private final Label errorNode;

    public FormField(String label, TextField input, boolean required) {
        setSpacing(4);
        this.inputNode = input;

        labelNode = new Label(label + (required ? " *" : ""));
        labelNode.getStyleClass().add("form-label");

        errorNode = new Label();
        errorNode.getStyleClass().add("form-error");
        errorNode.setVisible(false);
        errorNode.setManaged(false);

        getChildren().addAll(labelNode, inputNode, errorNode);
    }

    public void setError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            clearError();
        } else {
            errorNode.setText(errorMessage);
            errorNode.setVisible(true);
            errorNode.setManaged(true);
            if (!inputNode.getStyleClass().contains("input-error")) {
                inputNode.getStyleClass().add("input-error");
            }
        }
    }

    public void clearError() {
        errorNode.setText("");
        errorNode.setVisible(false);
        errorNode.setManaged(false);
        inputNode.getStyleClass().remove("input-error");
    }

    public TextField getInput() {
        return inputNode;
    }
}
