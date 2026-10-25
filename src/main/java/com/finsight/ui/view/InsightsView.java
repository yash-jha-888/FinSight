package com.finsight.ui.view;

import com.finsight.model.Transaction;
import com.finsight.model.TransactionType;
import com.finsight.service.Insight;
import com.finsight.ui.AppContext;
import com.finsight.ui.Fmt;
import com.finsight.ui.component.MeterBar;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.Comparator;

/** Plain-language observations, the largest expenses, and how you pay. */
public class InsightsView extends View {

    private final VBox insightList = new VBox();
    private final VBox largest = new VBox();
    private final VBox methods = new VBox(14);

    public InsightsView(AppContext ctx) {
        super(ctx);
    }

    @Override
    public String getTitle() {
        return "Insights";
    }

    @Override
    public String getSubtitle() {
        return "What the numbers are saying";
    }

    @Override
    protected Node build() {
        insightList.getStyleClass().add("insight-list");
        var left = new VBox(sectionHeader("Observations"), insightList);
        left.getStyleClass().add("section");

        var big = new VBox(sectionHeader("Largest expenses"), largest);
        big.getStyleClass().add("section");
        var how = new VBox(sectionHeader("How you paid"), methods);
        how.getStyleClass().add("section");
        var right = new VBox(36, big, how);

        var grid = new GridPane();
        grid.setHgap(56);
        var c1 = new ColumnConstraints();
        c1.setPercentWidth(55);
        var c2 = new ColumnConstraints();
        c2.setPercentWidth(45);
        grid.getColumnConstraints().addAll(c1, c2);
        grid.add(left, 0, 0);
        grid.add(right, 1, 0);
        return scrolling(page(grid));
    }

    @Override
    protected void reload() {
        var month = ctx.month();

        insightList.getChildren().clear();
        int n = 1;
        for (Insight i : ctx.insights().insightsFor(month)) {
            var num = label(String.format("%02d", n++), "insight-number");
            var headline = label(i.headline(), "insight-headline");
            headline.setWrapText(true);
            var detail = label(i.detail(), "insight-detail");
            detail.setWrapText(true);
            var text = new VBox(6, headline, detail);
            var row = new HBox(20, num, text);
            row.getStyleClass().addAll("insight", "tone-" + i.tone().name().toLowerCase());
            insightList.getChildren().add(row);
        }

        largest.getChildren().clear();
        var top = ctx.transactions().findByMonth(month).stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .sorted(Comparator.comparing(Transaction::getAmount).reversed())
                .limit(6)
                .toList();
        if (top.isEmpty()) largest.getChildren().add(label("No expenses yet.", "muted"));
        for (var t : top) {
            var desc = new VBox(2, label(t.getDescription().isBlank() ? t.getCategory().getName() : t.getDescription(),
                    "row-name"), label(t.getCategory().getName() + "  ·  " + Fmt.day(t.getDate()), "cell-secondary"));
            var row = new HBox(12, desc, spacer(), label(Fmt.money(t.getAmount()), "num"));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("plain-row");
            largest.getChildren().add(row);
        }

        methods.getChildren().clear();
        var byMethod = ctx.finance().spendingByPaymentMethod(month);
        BigDecimal total = byMethod.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        byMethod.forEach((m, amount) -> {
            double frac = amount.doubleValue() / total.doubleValue();
            var head = new HBox(label(m.getLabel(), "row-name"), spacer(),
                    label(Fmt.moneyWhole(amount) + "   " + Fmt.percent(frac), "num"));
            var bar = new MeterBar();
            bar.getStyleClass().add("meter-neutral");
            bar.setFraction(frac);
            bar.setMarker(-1);
            methods.getChildren().add(new VBox(8, head, bar));
        });
        if (byMethod.isEmpty()) methods.getChildren().add(label("No expenses yet.", "muted"));
    }
}
