package com.finsight.ui;

import com.finsight.db.TransactionDao;
import com.finsight.model.Categories;
import com.finsight.model.Transaction;
import javafx.geometry.Insets;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/** Shows total income, expenses and balance, and a pie chart of spending by category. */
public class SummaryTab extends Tab implements Refreshable {

    private TransactionDao dao;

    private Label incomeLabel = new Label();
    private Label expenseLabel = new Label();
    private Label balanceLabel = new Label();
    private PieChart pieChart = new PieChart();

    public SummaryTab(TransactionDao dao) {
        super("Summary");
        this.dao = dao;

        // three boxes across the top
        HBox totals = new HBox(16, makeBox("Income", incomeLabel), makeBox("Expenses", expenseLabel),
                makeBox("Balance", balanceLabel));

        pieChart.setTitle("Expenses by category");
        VBox.setVgrow(pieChart, Priority.ALWAYS);

        VBox content = new VBox(16, totals, pieChart);
        content.setPadding(new Insets(16));
        setContent(content);
    }

    private VBox makeBox(String title, Label valueLabel) {
        valueLabel.getStyleClass().add("summary-value");
        VBox box = new VBox(4, new Label(title), valueLabel);
        box.getStyleClass().add("summary-box");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    @Override
    public void refresh() {
        List<Transaction> transactions = dao.getAll();

        // getSignedAmount() is + for income and - for expenses, so adding them all gives the balance
        double income = 0;
        double expenses = 0;
        double balance = 0;
        for (Transaction t : transactions) {
            if (t.getType().equals("Income")) {
                income += t.getAmount();
            } else {
                expenses += t.getAmount();
            }
            balance += t.getSignedAmount();
        }
        incomeLabel.setText(Money.format(income));
        expenseLabel.setText(Money.format(expenses));
        balanceLabel.setText(Money.format(balance));

        // pie chart: one slice for each expense category that has spending
        pieChart.getData().clear();
        for (String category : Categories.EXPENSE) {
            double spent = 0;
            for (Transaction t : transactions) {
                if (t.getType().equals("Expense") && t.getCategory().equals(category)) {
                    spent += t.getAmount();
                }
            }
            if (spent > 0) {
                pieChart.getData().add(new PieChart.Data(category, spent));
            }
        }
    }
}
