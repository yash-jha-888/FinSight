package com.finsight.ui.dialog;

import com.finsight.model.Budget;
import com.finsight.model.Category;
import com.finsight.ui.Fmt;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Set or change the monthly limit for one expense category. */
public class BudgetDialog extends FormDialog<Budget> {

    private final long id;
    private final YearMonth month;
    private final ComboBox<Category> category;
    private final TextField limit = new TextField();

    public BudgetDialog(Window owner, List<Category> expenseCategories, YearMonth month, Budget existing) {
        super(owner, existing == null ? "Set a budget" : "Edit budget");
        this.id = existing == null ? 0 : existing.getId();
        this.month = month;
        category = new ComboBox<>(FXCollections.observableArrayList(expenseCategories));
        category.setMaxWidth(Double.MAX_VALUE);
        limit.setPromptText("Monthly limit");
        limit.getStyleClass().add("amount-field");

        addField("Month", new Label(Fmt.month(month)));
        addField("Category", category);
        addField("Limit", limit);

        if (existing != null) {
            category.setValue(existing.getCategory());
            category.setDisable(true);
            limit.setText(existing.getLimit().toPlainString());
        }
        setOnShown(e -> (existing == null ? category : limit).requestFocus());
    }

    private BigDecimal parsedLimit() {
        try {
            return new BigDecimal(limit.getText().replace(",", "").replace("₹", "").strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Limit must be a number, like 5000.");
        }
    }

    @Override
    protected String isValid() {
        if (category.getValue() == null) return "Pick a category.";
        if (parsedLimit().signum() <= 0) return "Limit must be greater than zero.";
        return null;
    }

    @Override
    protected Budget buildResult() {
        return new Budget(id, category.getValue(), month, parsedLimit());
    }
}
