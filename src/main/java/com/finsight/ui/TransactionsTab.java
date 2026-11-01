package com.finsight.ui;

import com.finsight.App;
import com.finsight.db.TransactionDao;
import com.finsight.model.Categories;
import com.finsight.model.Expense;
import com.finsight.model.Income;
import com.finsight.model.Transaction;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/** A form for adding a transaction, and a table of every transaction. */
public class TransactionsTab extends Tab implements Refreshable {

    private App app;
    private TransactionDao transactionDao;

    // each row of the table is an array of texts, one per column
    private TableView<String[]> table = new TableView<>();
    private List<Transaction> transactions; // the objects behind the table rows, in the same order

    private ComboBox<String> typeBox = new ComboBox<>();
    private TextField amountField = new TextField();
    private DatePicker datePicker = new DatePicker(LocalDate.now());
    private ComboBox<String> categoryBox = new ComboBox<>();
    private TextField descriptionField = new TextField();
    private ComboBox<String> paymentBox = new ComboBox<>();

    public TransactionsTab(App app, TransactionDao transactionDao) {
        super("Transactions");
        this.app = app;
        this.transactionDao = transactionDao;

        String[] titles = {"Date", "Type", "Category", "Description", "Paid with", "Amount"};
        for (int i = 0; i < titles.length; i++) {
            final int index = i; // the lambda below needs a variable that never changes
            TableColumn<String[], String> column = new TableColumn<>(titles[i]);
            // for each row, this column shows the text at position "index"
            column.setCellValueFactory(row -> new SimpleStringProperty(row.getValue()[index]));
            table.getColumns().add(column);
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        typeBox.getItems().addAll("Expense", "Income");
        typeBox.setValue("Expense");
        typeBox.setOnAction(e -> updateCategoryList());
        paymentBox.getItems().addAll(Categories.PAYMENT_METHODS);
        paymentBox.setValue("UPI");
        updateCategoryList();

        amountField.setPromptText("Amount");
        amountField.setPrefWidth(100);
        descriptionField.setPromptText("Description");

        Button addButton = new Button("Add");
        addButton.setOnAction(e -> addTransaction());
        Button deleteButton = new Button("Delete selected");
        deleteButton.setOnAction(e -> deleteTransaction());

        // the form is two rows of fields above the table
        HBox row1 = new HBox(10, new Label("Type"), typeBox, new Label("Amount"), amountField,
                new Label("Date"), datePicker);
        HBox row2 = new HBox(10, new Label("Category"), categoryBox, descriptionField,
                new Label("Paid with"), paymentBox, addButton, deleteButton);
        row1.setAlignment(Pos.CENTER_LEFT);
        row2.setAlignment(Pos.CENTER_LEFT);

        VBox.setVgrow(table, Priority.ALWAYS);
        VBox content = new VBox(10, row1, row2, table);
        content.setPadding(new Insets(16));
        setContent(content);
    }

    // income and expenses have different categories; payment method is only for expenses
    private void updateCategoryList() {
        categoryBox.getItems().clear();
        if (typeBox.getValue().equals("Income")) {
            categoryBox.getItems().addAll(Categories.INCOME);
            paymentBox.setDisable(true);
        } else {
            categoryBox.getItems().addAll(Categories.EXPENSE);
            paymentBox.setDisable(false);
        }
        categoryBox.getSelectionModel().selectFirst();
    }

    @Override
    public void refresh() {
        transactions = transactionDao.getAll();
        table.getItems().clear();
        for (Transaction t : transactions) {
            table.getItems().add(new String[]{
                    t.getDate().toString(),
                    t.getType(),
                    t.getCategory(),
                    t.getDescription(),
                    t.getPaymentMethod(),              // polymorphism: empty for income
                    Money.format(t.getSignedAmount())  // polymorphism: + for income, - for expenses
            });
        }
    }

    private void addTransaction() {
        try {
            double amount = Double.parseDouble(amountField.getText());
            Transaction t;
            if (typeBox.getValue().equals("Income")) {
                t = new Income(0, amount, datePicker.getValue(), categoryBox.getValue(),
                        descriptionField.getText());
            } else {
                t = new Expense(0, amount, datePicker.getValue(), categoryBox.getValue(),
                        descriptionField.getText(), paymentBox.getValue());
            }
            transactionDao.add(t);
            amountField.clear();
            descriptionField.clear();
            app.refreshAll();
        } catch (NumberFormatException e) {
            showError("Amount must be a number, like 250 or 99.50");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage()); // for example "Amount must be more than zero"
        }
    }

    private void deleteTransaction() {
        int row = table.getSelectionModel().getSelectedIndex();
        if (row < 0) {
            showError("Select a transaction to delete first.");
            return;
        }
        transactionDao.delete(transactions.get(row).getId());
        app.refreshAll();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }
}
