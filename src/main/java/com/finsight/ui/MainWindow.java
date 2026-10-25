package com.finsight.ui;

import com.finsight.ui.view.BudgetsView;
import com.finsight.ui.view.CategoriesView;
import com.finsight.ui.view.InsightsView;
import com.finsight.ui.view.LedgerView;
import com.finsight.ui.view.OverviewView;
import com.finsight.ui.view.View;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.YearMonth;
import java.util.List;

/** The window frame: sidebar navigation, a header with the month switcher, and the current view. */
public class MainWindow {

    private final AppContext ctx;
    private final List<View> views;
    private final BorderPane root = new BorderPane();
    private final StackPane content = new StackPane();
    private final Label title = new Label();
    private final Label subtitle = new Label();
    private final Label monthLabel = new Label();
    private final ToggleGroup nav = new ToggleGroup();
    private View current;

    public MainWindow(AppContext ctx) {
        this.ctx = ctx;
        this.views = List.of(new OverviewView(ctx), new LedgerView(ctx), new BudgetsView(ctx),
                new InsightsView(ctx), new CategoriesView(ctx));

        root.getStyleClass().add("app");
        root.setLeft(sidebar());
        var main = new BorderPane(content);
        main.setTop(header());
        main.getStyleClass().add("main");
        root.setCenter(main);

        ctx.monthProperty().addListener((obs, o, n) -> {
            monthLabel.setText(Fmt.month(n));
            current.refresh();
        });
        ctx.revisionProperty().addListener((obs, o, n) -> current.refresh());
        monthLabel.setText(Fmt.month(ctx.month()));
        show(views.get(0));
        ((ToggleButton) nav.getToggles().get(0)).setSelected(true);
    }

    public Scene createScene() {
        var scene = new Scene(root, 1320, 840);
        scene.getStylesheets().add(MainWindow.class.getResource("/com/finsight/css/finsight.css").toExternalForm());
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
                () -> LedgerView.addNew(ctx, scene.getWindow()));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.SHORTCUT_DOWN),
                () -> shiftMonth(-1));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.SHORTCUT_DOWN),
                () -> shiftMonth(1));
        for (int i = 0; i < views.size(); i++) {
            int index = i;
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.getKeyCode(String.valueOf(i + 1)),
                    KeyCombination.SHORTCUT_DOWN), () -> ((ToggleButton) nav.getToggles().get(index)).setSelected(true));
        }
        return scene;
    }

    /** Switches theme by adding or removing the "dark" class on the root. */
    public void setDark(boolean dark) {
        root.getStyleClass().remove("dark");
        if (dark) root.getStyleClass().add("dark");
    }

    public AppContext context() {
        return ctx;
    }

    public void showView(int index) {
        ((ToggleButton) nav.getToggles().get(index)).setSelected(true);
    }

    private Node sidebar() {
        var mark = new Label("FinSight");
        mark.getStyleClass().add("wordmark");
        var tagline = new Label("Personal ledger");
        tagline.getStyleClass().add("tagline");
        var brand = new VBox(2, mark, tagline);
        brand.getStyleClass().add("brand");

        var items = new VBox(2);
        items.getStyleClass().add("nav");
        for (int i = 0; i < views.size(); i++) {
            var view = views.get(i);
            var num = new Label(String.format("%02d", i + 1));
            num.getStyleClass().add("nav-number");
            var name = new Label(view.getTitle());
            name.getStyleClass().add("nav-name");
            var graphic = new HBox(14, num, name);
            graphic.setAlignment(Pos.CENTER_LEFT);

            var item = new ToggleButton();
            item.setGraphic(graphic);
            item.setToggleGroup(nav);
            item.setMaxWidth(Double.MAX_VALUE);
            item.getStyleClass().add("nav-item");
            item.setUserData(view);
            items.getChildren().add(item);
        }
        nav.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == null) {
                o.setSelected(true);
            } else {
                show((View) n.getUserData());
            }
        });

        var theme = new ToggleButton("Dark mode");
        theme.getStyleClass().add("theme-toggle");
        theme.selectedProperty().addListener((obs, o, dark) -> {
            setDark(dark);
            theme.setText(dark ? "Light mode" : "Dark mode");
        });
        var footer = new VBox(10, theme, new Label("Ctrl+N  new entry") {{ getStyleClass().add("shortcut"); }},
                new Label("Ctrl+← →  change month") {{ getStyleClass().add("shortcut"); }});
        footer.getStyleClass().add("sidebar-footer");

        var spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        var side = new VBox(brand, items, spacer, footer);
        side.getStyleClass().add("sidebar");
        return side;
    }

    private Node header() {
        title.getStyleClass().add("page-title");
        subtitle.getStyleClass().add("page-subtitle");
        var titles = new VBox(4, title, subtitle);

        var prev = new Button("‹");
        prev.getStyleClass().add("month-arrow");
        prev.setOnAction(e -> shiftMonth(-1));
        var next = new Button("›");
        next.getStyleClass().add("month-arrow");
        next.setOnAction(e -> shiftMonth(1));
        monthLabel.getStyleClass().add("month-label");
        var thisMonth = new Button("Today");
        thisMonth.getStyleClass().add("link-button");
        thisMonth.setOnAction(e -> ctx.monthProperty().set(YearMonth.from(ctx.finance().today())));
        var switcher = new HBox(6, prev, monthLabel, next, thisMonth);
        switcher.setAlignment(Pos.CENTER);
        switcher.getStyleClass().add("month-switcher");

        var add = new Button("New entry");
        add.getStyleClass().add("primary");
        add.setOnAction(e -> LedgerView.addNew(ctx, root.getScene().getWindow()));

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        var bar = new HBox(24, titles, spacer, switcher, add);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("header");
        return bar;
    }

    private void shiftMonth(int delta) {
        ctx.monthProperty().set(ctx.month().plusMonths(delta));
    }

    private void show(View view) {
        current = view;
        title.setText(view.getTitle());
        subtitle.setText(view.getSubtitle());
        content.getChildren().setAll(view.getRoot());
        view.refresh();
    }
}
