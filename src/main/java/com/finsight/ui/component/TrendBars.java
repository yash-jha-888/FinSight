package com.finsight.ui.component;

import com.finsight.service.MonthlySummary;
import com.finsight.ui.Fmt;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Income and spending per month side by side. Drawn by hand rather than with
 * {@code BarChart} so the spacing, labels and colours match the rest of the app.
 */
public class TrendBars extends Region {

    private static final double LABEL_H = 22;
    private static final double TOP_PAD = 18;

    private List<MonthlySummary> data = List.of();
    private final List<Region> incomeBars = new ArrayList<>();
    private final List<Region> expenseBars = new ArrayList<>();
    private final List<Label> monthLabels = new ArrayList<>();
    private final List<Label> netLabels = new ArrayList<>();
    private final Line baseline = new Line();

    public TrendBars() {
        getStyleClass().add("trend-bars");
        baseline.getStyleClass().add("trend-baseline");
        setMinHeight(200);
        setPrefHeight(210);
    }

    public void setData(List<MonthlySummary> months) {
        this.data = months;
        getChildren().clear();
        incomeBars.clear();
        expenseBars.clear();
        monthLabels.clear();
        netLabels.clear();
        getChildren().add(baseline);
        for (int i = 0; i < months.size(); i++) {
            var m = months.get(i);
            boolean current = i == months.size() - 1;

            var inc = new Region();
            inc.getStyleClass().add("trend-income");
            Tooltip.install(inc, new Tooltip("Income " + Fmt.money(m.income())));
            var exp = new Region();
            exp.getStyleClass().addAll("trend-expense", current ? "current" : "past");
            Tooltip.install(exp, new Tooltip("Spent " + Fmt.money(m.expense())));

            var label = new Label(Fmt.monthShort(m.month()));
            label.getStyleClass().addAll("trend-label", current ? "current" : "past");
            var net = new Label(m.transactionCount() == 0 ? "" : (m.net().signum() >= 0 ? "+" : "−")
                    + Fmt.moneyCompact(m.net().abs()).substring(0));
            net.getStyleClass().addAll("trend-net", m.net().signum() >= 0 ? "positive" : "negative");

            incomeBars.add(inc);
            expenseBars.add(exp);
            monthLabels.add(label);
            netLabels.add(net);
            getChildren().addAll(inc, exp, label, net);
        }
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        if (data.isEmpty()) return;
        double w = getWidth() - snappedLeftInset() - snappedRightInset();
        double h = getHeight() - snappedTopInset() - snappedBottomInset();
        double left = snappedLeftInset();
        double chartH = h - LABEL_H * 2 - TOP_PAD;
        double baseY = snappedTopInset() + TOP_PAD + chartH;

        BigDecimal max = data.stream()
                .map(m -> m.income().max(m.expense()))
                .max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        if (max.signum() == 0) max = BigDecimal.ONE;

        double slot = w / data.size();
        double barW = Math.min(26, slot * 0.22);
        double gap = 4;

        for (int i = 0; i < data.size(); i++) {
            var m = data.get(i);
            double cx = left + slot * i + slot / 2;
            double ih = Math.round(chartH * m.income().doubleValue() / max.doubleValue());
            double eh = Math.round(chartH * m.expense().doubleValue() / max.doubleValue());
            incomeBars.get(i).resizeRelocate(Math.round(cx - gap / 2 - barW), baseY - ih, barW, ih);
            expenseBars.get(i).resizeRelocate(Math.round(cx + gap / 2), baseY - eh, barW, eh);

            layoutInArea(monthLabels.get(i), left + slot * i, baseY + 6, slot, LABEL_H, 0,
                    null, javafx.geometry.HPos.CENTER, VPos.TOP);
            layoutInArea(netLabels.get(i), left + slot * i, baseY + 6 + LABEL_H, slot, LABEL_H, 0,
                    null, javafx.geometry.HPos.CENTER, VPos.TOP);
        }
        baseline.setStartX(left);
        baseline.setEndX(left + w);
        baseline.setStartY(baseY + 0.5);
        baseline.setEndY(baseY + 0.5);
    }
}
