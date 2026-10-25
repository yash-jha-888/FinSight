package com.finsight.ui.dialog;

import com.finsight.model.Category;
import com.finsight.model.TransactionType;
import javafx.collections.FXCollections;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Window;

/** Add or rename a category and pick its colour. */
public class CategoryDialog extends FormDialog<Category> {

    private final long id;
    private final TextField name = new TextField();
    private final ComboBox<TransactionType> type =
            new ComboBox<>(FXCollections.observableArrayList(TransactionType.values()));
    private final ColorPicker color = new ColorPicker();

    public CategoryDialog(Window owner, Category existing, TransactionType defaultType) {
        super(owner, existing == null ? "New category" : "Edit category");
        this.id = existing == null ? 0 : existing.getId();
        type.setMaxWidth(Double.MAX_VALUE);
        type.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(TransactionType t) {
                return t == null ? "" : t.getLabel();
            }

            @Override
            public TransactionType fromString(String s) {
                return null;
            }
        });

        addField("Name", name);
        addField("Type", type);
        addField("Colour", color);

        if (existing != null) {
            name.setText(existing.getName());
            type.setValue(existing.getType());
            type.setDisable(true); // changing type would break existing transactions
            color.setValue(Color.web(existing.getColorHex()));
        } else {
            type.setValue(defaultType);
            color.setValue(Color.web("#5D7A8C"));
        }
        setOnShown(e -> name.requestFocus());
    }

    @Override
    protected String isValid() {
        if (name.getText().isBlank()) return "Give the category a name.";
        return null;
    }

    @Override
    protected Category buildResult() {
        Color c = color.getValue();
        String hex = String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed() * 255), (int) Math.round(c.getGreen() * 255), (int) Math.round(c.getBlue() * 255));
        return new Category(id, name.getText(), type.getValue(), hex);
    }
}
