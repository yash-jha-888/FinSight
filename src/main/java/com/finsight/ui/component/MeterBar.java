package com.finsight.ui.component;

import javafx.scene.layout.Region;

/**
 * A thin horizontal bar showing how much of a budget is used, with an
 * optional tick for where spending "should" be by today.
 */
public class MeterBar extends Region {

    private final Region fill = new Region();
    private final Region marker = new Region();
    private double fraction;
    private double markerAt = -1;

    public MeterBar() {
        getStyleClass().add("meter");
        fill.getStyleClass().add("meter-fill");
        marker.getStyleClass().add("meter-marker");
        marker.setManaged(false);
        getChildren().addAll(fill, marker);
        setMinHeight(6);
        setPrefHeight(6);
        setMaxHeight(6);
    }

    public void setFraction(double fraction) {
        this.fraction = Math.max(0, fraction);
        fill.getStyleClass().removeAll("meter-ok", "meter-warn", "meter-over");
        fill.getStyleClass().add(fraction > 1 ? "meter-over" : fraction >= 0.8 ? "meter-warn" : "meter-ok");
        requestLayout();
    }

    /** Position of the pace tick from 0 to 1, or negative to hide it. */
    public void setMarker(double at) {
        this.markerAt = at;
        marker.setVisible(at >= 0 && at <= 1);
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        fill.resizeRelocate(0, 0, Math.round(w * Math.min(1, fraction)), h);
        double x = Math.round(w * Math.max(0, markerAt));
        marker.resizeRelocate(x - 1, -4, 2, h + 8);
    }
}
