package com.finsight.ui.view;

import com.finsight.ui.AppContext;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * One screen of the app.
 *
 * <p><b>OOP: abstraction and polymorphism.</b> The main window keeps a list of
 * {@code View}s and only ever calls {@link #getRoot()} and {@link #refresh()}.
 * Each subclass decides what it shows and how it loads its data.
 */
public abstract class View {

    protected final AppContext ctx;
    private Node root;
    private boolean stale = true;

    protected View(AppContext ctx) {
        this.ctx = ctx;
        // Any change of month or data marks every view as needing a reload.
        ctx.monthProperty().addListener((obs, o, n) -> stale = true);
        ctx.revisionProperty().addListener((obs, o, n) -> stale = true);
    }

    /** Name shown in the sidebar and page header. */
    public abstract String getTitle();

    /** One line under the page title. */
    public abstract String getSubtitle();

    /** Builds the layout once. Called lazily the first time the view is shown. */
    protected abstract Node build();

    /** Reloads data from the services into the existing layout. */
    protected abstract void reload();

    public final Node getRoot() {
        if (root == null) {
            root = build();
        }
        return root;
    }

    /** Reloads only when something has changed since the last time. */
    public final void refresh() {
        getRoot();
        if (stale) {
            reload();
            stale = false;
        }
    }

    // Small layout helpers shared by the screens.

    protected static ScrollPane scrolling(Node content) {
        var scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        return scroll;
    }

    protected static VBox page(Node... children) {
        var box = new VBox(36, children);
        box.getStyleClass().add("page");
        return box;
    }

    /** A small uppercase label with a rule under it, used to title each section. */
    protected static Node sectionHeader(String text, Node... trailing) {
        var label = new Label(text);
        label.getStyleClass().add("section-title");
        var row = new HBox(12, label, spacer());
        row.getChildren().addAll(trailing);
        row.getStyleClass().add("section-header");
        return row;
    }

    protected static Region spacer() {
        var r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    protected static Label label(String text, String... styles) {
        var l = new Label(text);
        l.getStyleClass().addAll(styles);
        return l;
    }
}
