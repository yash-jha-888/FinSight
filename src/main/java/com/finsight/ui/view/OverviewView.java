package com.finsight.ui.view;

import com.finsight.alert.Alert;
import com.finsight.model.TransactionType;
import com.finsight.ui.AppContext;
import com.finsight.ui.Fmt;
import com.finsight.ui.component.CategoryBars;
import com.finsight.ui.component.Figure;
import com.finsight.ui.component.SpendCurve;
import com.finsight.ui.component.TrendBars;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.YearMonth;

/** The dashboard: headline numbers, alerts, and three charts for the selected month. */
public class OverviewView extends View {

    private final Figure net = new Figure("Left over this month", true);
    private final Figure income = new Figure("Income", false);
    private final Figure spent = new Figure("Spent", false);
    private final Figure kept = new Figure("Savings rate", false);
    private final Figure balance = new Figure("All-time balance", false);

    private final VBox alertList = new VBox();
    private final CategoryBars categoryBars = new CategoryBars();
    private final SpendCurve curve = new SpendCurve();
    private final TrendBars trend = new TrendBars();
    private final Label curveNote = label("", "chart-note");

    public OverviewView(AppContext ctx) {
        super(ctx);
    }

    @Override
    public String getTitle() {
        return "Overview";
    }

    @Override
    public String getSubtitle() {
        return "Where the month stands";
    }

    @Override
    protected Node build() {
        // Headline: one big number, then a row of supporting figures divided by rules.
        var figures = new HBox(income, rule(), spent, rule(), kept, rule(), balance);
        figures.getStyleClass().add("figure-row");
        for (var f : new Figure[]{income, spent, kept, balance}) {
            HBox.setHgrow(f, Priority.ALWAYS);
            f.setMaxWidth(Double.MAX_VALUE);
        }

        alertList.getStyleClass().add("alert-list");
        var alerts = new VBox(sectionHeader("Needs attention"), alertList);
        alerts.getStyleClass().add("section");

        var headline = new GridPane();
        headline.setHgap(48);
        var c1 = new ColumnConstraints();
        c1.setPercentWidth(58);
        var c2 = new ColumnConstraints();
        c2.setPercentWidth(42);
        headline.getColumnConstraints().addAll(c1, c2);
        headline.add(new VBox(20, net, figures), 0, 0);
        headline.add(alerts, 1, 0);

        var curveBox = new VBox(sectionHeader("Spending through the month", legend()), curve, curveNote);
        curveBox.getStyleClass().add("section");
        var catBox = new VBox(sectionHeader("By category"), categoryBars);
        catBox.getStyleClass().add("section");

        var charts = new GridPane();
        charts.setHgap(48);
        var h1 = new ColumnConstraints();
        h1.setPercentWidth(55);
        var h2 = new ColumnConstraints();
        h2.setPercentWidth(45);
        charts.getColumnConstraints().addAll(h1, h2);
        charts.add(curveBox, 0, 0);
        charts.add(catBox, 1, 0);

        var trendBox = new VBox(sectionHeader("Six months", trendLegend()), trend);
        trendBox.getStyleClass().add("section");

        return scrolling(page(headline, charts, trendBox));
    }

    @Override
    protected void reload() {
        YearMonth month = ctx.month();
        var f = ctx.finance();
        var s = f.monthlySummary(month);

        net.set(Fmt.money(s.net()), s.transactionCount() + " transactions in " + Fmt.month(month),
                s.net().signum() < 0 ? "negative" : null);
        income.set(Fmt.moneyWhole(s.income()), "", null);
        spent.set(Fmt.moneyWhole(s.expense()), "", null);
        kept.set(Fmt.percent(s.savingsRate()), "of income", s.savingsRate() >= 0.2 ? "positive"
                : s.savingsRate() < 0 ? "negative" : null);
        balance.set(Fmt.moneyWhole(f.balance()), "", null);

        alertList.getChildren().clear();
        var alerts = f.alerts(month);
        if (alerts.isEmpty()) {
            alertList.getChildren().add(label("All budgets are on track.", "muted", "alert-empty"));
        } else {
            alerts.stream().limit(4).forEach(a -> alertList.getChildren().add(alertRow(a)));
        }

        categoryBars.setData(f.categoryTotals(month, TransactionType.EXPENSE));

        var today = f.today();
        boolean isCurrent = month.equals(YearMonth.from(today));
        int upTo = isCurrent ? today.getDayOfMonth() : month.lengthOfMonth();
        var currentDays = f.dailySpending(month);
        var previousDays = f.dailySpending(month.minusMonths(1));
        curve.setData(currentDays, upTo, previousDays);

        var prevToSameDay = java.util.Arrays.stream(previousDays).limit(upTo)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        var diff = s.expense().subtract(prevToSameDay);
        curveNote.setText(prevToSameDay.signum() == 0 ? "" :
                Fmt.moneyWhole(diff.abs()) + (diff.signum() > 0 ? " more" : " less") + " than "
                        + Fmt.monthShort(month.minusMonths(1)) + " by day " + upTo + ".");

        trend.setData(f.monthlyTrend(month, 6));
    }

    private static Node alertRow(Alert a) {
        var title = label(a.title(), "alert-title");
        var msg = label(a.message(), "alert-message");
        msg.setWrapText(true);
        var box = new VBox(2, title, msg);
        box.getStyleClass().addAll("alert-row", "alert-" + a.severity().name().toLowerCase());
        return box;
    }

    private static Region rule() {
        var r = new Region();
        r.getStyleClass().add("vrule");
        return r;
    }

    private Node legend() {
        var thisMonth = label("This month", "legend", "legend-current");
        var last = label("Last month", "legend", "legend-previous");
        return new HBox(16, thisMonth, last);
    }

    private Node trendLegend() {
        return new HBox(16, label("Income", "legend", "legend-income"), label("Spent", "legend", "legend-expense"));
    }
}
