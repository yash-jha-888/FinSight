package com.finsight.ui.component;

import com.finsight.ui.Fmt;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.util.StringConverter;

import java.math.BigDecimal;

/**
 * Running total of spending through the month, with last month as a faint
 * dashed reference line, so you can see at a glance if you are ahead or behind.
 */
public class SpendCurve extends LineChart<Number, Number> {

    private final NumberAxis xAxis;
    private final NumberAxis yAxis;

    public SpendCurve() {
        super(new NumberAxis(), new NumberAxis());
        xAxis = (NumberAxis) getXAxis();
        yAxis = (NumberAxis) getYAxis();
        getStyleClass().add("spend-curve");
        setCreateSymbols(false);
        setAnimated(false);
        setLegendVisible(false);
        setHorizontalGridLinesVisible(true);
        setVerticalGridLinesVisible(false);
        setAlternativeRowFillVisible(false);
        setHorizontalZeroLineVisible(false);
        setVerticalZeroLineVisible(false);

        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
                xAxis.setMinorTickVisible(false);
        xAxis.setTickMarkVisible(false);
        xAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number n) {
                int d = n.intValue();
                return d >= 1 && (d - 1) % 7 == 0 ? String.valueOf(d) : "";
            }

            @Override
            public Number fromString(String s) {
                return 0;
            }
        });
        xAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);
        yAxis.setTickMarkVisible(false);
        yAxis.setForceZeroInRange(true);
        yAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number n) {
                return Fmt.moneyCompact(BigDecimal.valueOf(n.doubleValue()));
            }

            @Override
            public Number fromString(String s) {
                return 0;
            }
        });
        setPrefHeight(240);
        setMinHeight(200);
    }

    /**
     * @param current daily spending this month
     * @param upToDay last day to draw for this month (today), or the month length for past months
     * @param previous daily spending last month
     */
    public void setData(BigDecimal[] current, int upToDay, BigDecimal[] previous) {
        getData().clear();
        xAxis.setUpperBound(Math.max(current.length, previous.length));

        var prev = cumulative(previous, previous.length);
        var now = cumulative(current, upToDay);
        getData().add(prev);
        getData().add(now);
        prev.getNode().getStyleClass().add("series-previous");
        now.getNode().getStyleClass().add("series-current");
    }

    private static XYChart.Series<Number, Number> cumulative(BigDecimal[] days, int upTo) {
        var series = new XYChart.Series<Number, Number>();
        BigDecimal running = BigDecimal.ZERO;
        series.getData().add(new XYChart.Data<>(0, 0));
        for (int d = 0; d < Math.min(upTo, days.length); d++) {
            running = running.add(days[d]);
            series.getData().add(new XYChart.Data<>(d + 1, running.doubleValue()));
        }
        return series;
    }
}
