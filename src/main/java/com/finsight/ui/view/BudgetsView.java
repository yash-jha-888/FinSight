package com.finsight.ui.view;

import com.finsight.alert.BudgetStatus;
import com.finsight.model.Budget;
import com.finsight.model.TransactionType;
import com.finsight.service.CategoryTotal;
import com.finsight.ui.AppContext;
import com.finsight.ui.Fmt;
import com.finsight.ui.component.MeterBar;
import com.finsight.ui.dialog.BudgetDialog;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Set;
import java.util.stream.Collectors;

/** Monthly limits per category, how much of each is used, and spending that has no budget. */
public class BudgetsView extends View {

    private final Label totalLine = label("", "budget-total");
    private final MeterBar totalMeter = new MeterBar();
    private final GridPane list = new GridPane();
    private final VBox unbudgeted = new VBox(8);

    public BudgetsView(AppContext ctx) {
        super(ctx);
    }

    @Override
    public String getTitle() {
        return "Budgets";
    }

    @Override
    public String getSubtitle() {
        return "Limits for the month and how close you are";
    }

    @Override
    protected Node build() {
        var add = new Button("Set a budget");
        add.getStyleClass().add("secondary");
        add.setOnAction(e -> openDialog(null));
        var copy = new Button("Copy last month's");
        copy.getStyleClass().add("link-button");
        copy.setOnAction(e -> {
            ctx.budgets().copyMonth(ctx.month().minusMonths(1), ctx.month());
            ctx.dataChanged();
        });

        var summary = new VBox(10, totalLine, totalMeter, label(
                "The tick on each bar marks how far through the month we are. A bar past its tick is spending ahead of pace.",
                "hint"));
        summary.getStyleClass().add("budget-summary");

        list.getStyleClass().add("budget-list");
        list.setHgap(0);
        list.setVgap(0);
        var c0 = new ColumnConstraints();
        c0.setMinWidth(170);
        var c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        c1.setMinWidth(160);
        var c2 = new ColumnConstraints();
        c2.setMinWidth(170);
        c2.setHalignment(HPos.RIGHT);
        var c3 = new ColumnConstraints();
        c3.setMinWidth(150);
        c3.setHalignment(HPos.RIGHT);
        var c4 = new ColumnConstraints();
        c4.setMinWidth(110);
        c4.setHalignment(HPos.RIGHT);
        list.getColumnConstraints().addAll(c0, c1, c2, c3, c4);

        var budgets = new VBox(sectionHeader("This month's budgets", copy, add), list);
        budgets.getStyleClass().add("section");
        var other = new VBox(sectionHeader("Spending without a budget"), unbudgeted);
        other.getStyleClass().add("section");

        return scrolling(page(summary, budgets, other));
    }

    @Override
    protected void reload() {
        YearMonth month = ctx.month();
        var statuses = ctx.finance().budgetStatuses(month);

        BigDecimal limit = statuses.stream().map(s -> s.budget().getLimit()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal spent = statuses.stream().map(BudgetStatus::spent).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (limit.signum() > 0) {
            totalLine.setText(Fmt.moneyWhole(spent) + " of " + Fmt.moneyWhole(limit) + " budgeted");
            totalMeter.setFraction(spent.doubleValue() / limit.doubleValue());
            totalMeter.setMarker(statuses.get(0).monthElapsedFraction());
            totalMeter.setVisible(true);
        } else {
            totalLine.setText("No budgets for " + Fmt.month(month) + " yet");
            totalMeter.setVisible(false);
        }

        list.getChildren().clear();
        int row = 0;
        for (var s : statuses) {
            addRow(row++, s);
        }
        if (statuses.isEmpty()) {
            list.add(label("Set a limit for a category, or copy last month's budgets.", "muted"), 0, 0, 5, 1);
        }

        Set<Long> budgeted = statuses.stream().map(s -> s.budget().getCategory().getId()).collect(Collectors.toSet());
        unbudgeted.getChildren().clear();
        var free = ctx.finance().categoryTotals(month, TransactionType.EXPENSE).stream()
                .filter(t -> !budgeted.contains(t.category().getId()))
                .toList();
        if (free.isEmpty()) {
            unbudgeted.getChildren().add(label("Every category you spent in has a budget.", "muted"));
        }
        for (CategoryTotal t : free) {
            var row2 = new HBox(12, dot(t.category().getColorHex()), label(t.category().getName(), "row-name"),
                    spacer(), label(Fmt.moneyWhole(t.total()), "num"));
            row2.setAlignment(Pos.CENTER_LEFT);
            row2.getStyleClass().add("plain-row");
            unbudgeted.getChildren().add(row2);
        }
    }

    private void addRow(int row, BudgetStatus s) {
        Budget b = s.budget();
        var name = new HBox(10, dot(b.getCategory().getColorHex()), label(b.getCategory().getName(), "row-name"));
        name.setAlignment(Pos.CENTER_LEFT);

        var meter = new MeterBar();
        meter.setFraction(s.usedFraction());
        meter.setMarker(s.monthElapsedFraction());
        meter.setMaxWidth(Double.MAX_VALUE);

        var amounts = label(Fmt.moneyWhole(s.spent()) + " / " + Fmt.moneyWhole(b.getLimit()), "num");

        BigDecimal remaining = b.remaining(s.spent());
        var state = remaining.signum() < 0
                ? label(Fmt.moneyWhole(remaining.negate()) + " over", "num", "negative")
                : label(Fmt.moneyWhole(remaining) + " left", "num", s.usedFraction() >= 0.8 ? "warning" : "muted");

        var edit = new Button("Edit");
        edit.getStyleClass().add("link-button");
        edit.setOnAction(e -> openDialog(b));
        var remove = new Button("Remove");
        remove.getStyleClass().addAll("link-button", "danger");
        remove.setOnAction(e -> {
            ctx.budgets().delete(b.getId());
            ctx.dataChanged();
        });
        var actions = new HBox(4, edit, remove);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Node[] cells = {name, meter, amounts, state, actions};
        for (int i = 0; i < cells.length; i++) {
            var cell = new HBox(cells[i]);
            cell.setAlignment(i >= 2 ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            HBox.setHgrow(cells[i], Priority.ALWAYS);
            cell.getStyleClass().add("budget-cell");
            list.add(cell, i, row);
        }
    }

    private static Circle dot(String hex) {
        var c = new Circle(4.5);
        c.setStyle("-fx-fill: " + hex + ";");
        return c;
    }

    private void openDialog(Budget existing) {
        var dialog = new BudgetDialog(list.getScene().getWindow(),
                ctx.categories().findByType(TransactionType.EXPENSE), ctx.month(), existing);
        dialog.showAndWait().ifPresent(b -> {
            ctx.budgets().save(b);
            ctx.dataChanged();
        });
    }
}
