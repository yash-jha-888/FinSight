package com.finsight.ui.dialog;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Base for the add and edit dialogs: a title, a two-column form and
 * Save/Cancel buttons, styled like the rest of the app.
 *
 * <p><b>OOP: inheritance and abstraction.</b> Subclasses add their fields and
 * implement {@link #isValid()} and {@link #buildResult()}; this class wires
 * up validation and the result converter once.
 */
public abstract class FormDialog<R> extends Dialog<R> {

    protected final GridPane form = new GridPane();
    private final Label error = new Label();
    private int rows;
    protected final ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);

    protected FormDialog(Window owner, String title) {
        initOwner(owner);
        initStyle(StageStyle.UTILITY);
        setTitle(title);

        var heading = new Label(title);
        heading.getStyleClass().add("dialog-title");
        form.getStyleClass().add("form");
        form.setHgap(16);
        form.setVgap(14);
        error.getStyleClass().add("form-error");
        error.setWrapText(true);

        var pane = getDialogPane();
        pane.getStyleClass().add("form-dialog");
        if (owner != null && owner.getScene() != null) {
            pane.getStylesheets().setAll(owner.getScene().getStylesheets());
            pane.getStyleClass().addAll(owner.getScene().getRoot().getStyleClass().filtered(c -> c.equals("dark")));
        }
        pane.setContent(new VBox(20, heading, form, error));
        pane.getButtonTypes().addAll(ButtonType.CANCEL, save);
        ((Button) pane.lookupButton(save)).getStyleClass().add("primary");

        // Block closing on Save while the form is invalid, and say why.
        pane.lookupButton(save).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            String problem = validate();
            if (problem != null) {
                error.setText(problem);
                e.consume();
            }
        });
        setResultConverter(bt -> bt == save ? buildResult() : null);
    }

    protected void addField(String label, Node field) {
        var l = new Label(label);
        l.getStyleClass().add("form-label");
        form.addRow(rows++, l, field);
    }

    /** Returns null when valid, otherwise a message for the user. */
    private String validate() {
        try {
            return isValid();
        } catch (RuntimeException ex) {
            return ex.getMessage();
        }
    }

    protected abstract String isValid();

    protected abstract R buildResult();
}
