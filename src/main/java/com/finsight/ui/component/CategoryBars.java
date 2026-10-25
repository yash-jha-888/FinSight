package com.finsight.ui.component;

import com.finsight.service.CategoryTotal;
import com.finsight.ui.Fmt;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Spending by category as ranked horizontal bars. Easier to compare than a
 * pie chart, and each bar uses its category's own colour.
 */
public class CategoryBars extends VBox {

    public CategoryBars() {
        super(12);
        getStyleClass().add("category-bars");
    }

    public void setData(List<CategoryTotal> totals) {
        getChildren().clear();
        if (totals.isEmpty()) {
            var empty = new Label("No spending recorded this month.");
            empty.getStyleClass().add("muted");
            getChildren().add(empty);
            return;
        }
        BigDecimal max = totals.get(0).total();
        BigDecimal sum = totals.stream().map(CategoryTotal::total).reduce(BigDecimal.ZERO, BigDecimal::add);

        for (var t : totals) {
            var name = fixed(new Label(t.category().getName()), 120, "bar-label");
            var bar = new Bar(t.total().divide(max, 4, RoundingMode.HALF_UP).doubleValue(), t.category().getColorHex());
            HBox.setHgrow(bar, Priority.ALWAYS);
            var amount = fixed(new Label(Fmt.moneyWhole(t.total())), 80, "num");
            amount.setAlignment(Pos.CENTER_RIGHT);
            var share = fixed(new Label(Fmt.percent(t.total().divide(sum, 4, RoundingMode.HALF_UP).doubleValue())),
                    36, "num", "muted");
            share.setAlignment(Pos.CENTER_RIGHT);

            var row = new HBox(14, name, bar, amount, share);
            row.setAlignment(Pos.CENTER_LEFT);
            getChildren().add(row);
        }
    }

    private static Label fixed(Label l, double width, String... styles) {
        l.setMinWidth(width);
        l.setPrefWidth(width);
        l.getStyleClass().addAll(styles);
        return l;
    }

    /** A bar whose filled part is a fraction of the available width. */
    private static final class Bar extends Region {

        private final Region fill = new Region();
        private final double fraction;

        Bar(double fraction, String colorHex) {
            this.fraction = fraction;
            fill.setStyle("-fx-background-color: " + colorHex + ";");
            getChildren().add(fill);
            setMinWidth(40);
            setPrefHeight(10);
            setMaxWidth(Double.MAX_VALUE);
        }

        @Override
        protected void layoutChildren() {
            fill.resizeRelocate(0, 0, Math.max(2, Math.round(getWidth() * fraction)), getHeight());
        }
    }
}
