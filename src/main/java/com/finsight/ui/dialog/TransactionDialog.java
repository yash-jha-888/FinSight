package com.finsight.ui.dialog;

import com.finsight.model.Category;
import com.finsight.model.Expense;
import com.finsight.model.Income;
import com.finsight.model.PaymentMethod;
import com.finsight.model.Transaction;
import com.finsight.model.TransactionType;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Add or edit an income or expense. */
public class TransactionDialog extends FormDialog<Transaction> {

    private final long id;
    private final List<Category> allCategories;

    private final ToggleGroup typeGroup = new ToggleGroup();
    private final ToggleButton expenseBtn = new ToggleButton("Expense");
    private final ToggleButton incomeBtn = new ToggleButton("Income");
    private final TextField amount = new TextField();
    private final DatePicker date = new DatePicker();
    private final ComboBox<Category> category = new ComboBox<>();
    private final TextField description = new TextField();
    private final ComboBox<PaymentMethod> method = new ComboBox<>(FXCollections.observableArrayList(PaymentMethod.values()));
    private final TextField source = new TextField();
    private final HBox detailHolder = new HBox();

    public TransactionDialog(Window owner, List<Category> categories, Transaction existing, LocalDate defaultDate) {
        super(owner, existing == null ? "New entry" : "Edit entry");
        this.id = existing == null ? 0 : existing.getId();
        this.allCategories = categories;

        expenseBtn.setToggleGroup(typeGroup);
        incomeBtn.setToggleGroup(typeGroup);
        incomeBtn.getStyleClass().add("last");
        var seg = new HBox(expenseBtn, incomeBtn);
        seg.getStyleClass().add("segmented");
        typeGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == null) {
                o.setSelected(true); // keep one selected
                return;
            }
            onTypeChanged();
        });

        date.setConverter(new javafx.util.converter.LocalDateStringConverter(
                java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"), null));
        amount.setPromptText("0.00");
        amount.getStyleClass().add("amount-field");
        description.setPromptText("What was it for?");
        source.setPromptText("Employer, client, ...");
        category.setMaxWidth(Double.MAX_VALUE);
        method.setMaxWidth(Double.MAX_VALUE);

        addField("Type", seg);
        addField("Amount", amount);
        addField("Date", date);
        addField("Category", category);
        addField("Description", description);
        addField("", detailHolder);

        if (existing != null) {
            (existing.getType() == TransactionType.INCOME ? incomeBtn : expenseBtn).setSelected(true);
            amount.setText(existing.getAmount().toPlainString());
            date.setValue(existing.getDate());
            description.setText(existing.getDescription());
            if (existing instanceof Expense e) method.setValue(e.getPaymentMethod());
            if (existing instanceof Income i) source.setText(i.getSource());
            category.setValue(existing.getCategory());
        } else {
            expenseBtn.setSelected(true);
            date.setValue(defaultDate);
            method.setValue(PaymentMethod.UPI);
        }
        setOnShown(e -> amount.requestFocus());
    }

    private TransactionType selectedType() {
        return incomeBtn.isSelected() ? TransactionType.INCOME : TransactionType.EXPENSE;
    }

    private void onTypeChanged() {
        var type = selectedType();
        var keep = category.getValue();
        category.setItems(FXCollections.observableArrayList(
                allCategories.stream().filter(c -> c.getType() == type).toList()));
        category.setValue(keep != null && keep.getType() == type ? keep : null);

        var label = (javafx.scene.control.Label) form.getChildren().get(form.getChildren().size() - 2);
        detailHolder.getChildren().setAll(type == TransactionType.EXPENSE ? method : source);
        HBox.setHgrow(method, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(source, javafx.scene.layout.Priority.ALWAYS);
        label.setText(type == TransactionType.EXPENSE ? "Paid with" : "Source");
    }

    private BigDecimal parsedAmount() {
        String text = amount.getText().replace(",", "").replace("₹", "").strip();
        if (text.isEmpty()) throw new IllegalArgumentException("Enter an amount.");
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a number, like 1250 or 99.50.");
        }
    }

    @Override
    protected String isValid() {
        if (parsedAmount().signum() <= 0) return "Amount must be greater than zero.";
        if (date.getValue() == null) return "Pick a date.";
        if (category.getValue() == null) return "Pick a category.";
        if (selectedType() == TransactionType.EXPENSE && method.getValue() == null) return "Pick a payment method.";
        return null;
    }

    @Override
    protected Transaction buildResult() {
        // A new object even when editing, so switching type works. The id makes it an update.
        return switch (selectedType()) {
            case INCOME -> new Income(id, parsedAmount(), date.getValue(), category.getValue(),
                    description.getText(), source.getText());
            case EXPENSE -> new Expense(id, parsedAmount(), date.getValue(), category.getValue(),
                    description.getText(), method.getValue());
        };
    }
}
