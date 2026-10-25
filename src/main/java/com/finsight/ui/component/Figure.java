package com.finsight.ui.component;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** A captioned number, like a line in a printed report: "SPENT" over "₹35,940". */
public class Figure extends VBox {

    private final Label value = new Label();
    private final Label note = new Label();

    public Figure(String caption, boolean large) {
        var cap = new Label(caption);
        cap.getStyleClass().add("figure-caption");
        value.getStyleClass().addAll("figure-value", large ? "figure-value-lg" : "figure-value-md");
        note.getStyleClass().add("figure-note");
        getChildren().addAll(cap, value, note);
        getStyleClass().add("figure");
    }

    /** @param tone extra style class for the value, such as "positive" or "negative"; may be null */
    public void set(String valueText, String noteText, String tone) {
        value.setText(valueText);
        note.setText(noteText);
        value.getStyleClass().removeAll("positive", "negative", "warning");
        if (tone != null) {
            value.getStyleClass().add(tone);
        }
    }
}
