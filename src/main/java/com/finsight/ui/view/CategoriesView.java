package com.finsight.ui.view;

import com.finsight.model.Category;
import com.finsight.model.TransactionType;
import com.finsight.ui.AppContext;
import com.finsight.ui.dialog.CategoryDialog;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/** Add, rename, recolour and remove categories. */
public class CategoriesView extends View {

    private final VBox expenseList = new VBox();
    private final VBox incomeList = new VBox();

    public CategoriesView(AppContext ctx) {
        super(ctx);
    }

    @Override
    public String getTitle() {
        return "Categories";
    }

    @Override
    public String getSubtitle() {
        return "How entries are grouped";
    }

    @Override
    protected Node build() {
        var grid = new GridPane();
        grid.setHgap(56);
        var c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        var c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);
        grid.add(column("Expense categories", expenseList, TransactionType.EXPENSE), 0, 0);
        grid.add(column("Income categories", incomeList, TransactionType.INCOME), 1, 0);
        return scrolling(page(grid));
    }

    private Node column(String title, VBox list, TransactionType type) {
        var add = new Button("Add");
        add.getStyleClass().add("link-button");
        add.setOnAction(e -> open(null, type));
        var box = new VBox(sectionHeader(title, add), list);
        box.getStyleClass().add("section");
        return box;
    }

    @Override
    protected void reload() {
        fill(expenseList, TransactionType.EXPENSE);
        fill(incomeList, TransactionType.INCOME);
    }

    private void fill(VBox list, TransactionType type) {
        list.getChildren().clear();
        for (Category c : ctx.categories().findByType(type)) {
            var swatch = new Rectangle(14, 14);
            swatch.setStyle("-fx-fill: " + c.getColorHex() + ";");
            var edit = new Button("Edit");
            edit.getStyleClass().add("link-button");
            edit.setOnAction(e -> open(c, type));
            var remove = new Button("Remove");
            remove.getStyleClass().addAll("link-button", "danger");
            remove.setOnAction(e -> remove(c));
            var row = new HBox(14, swatch, label(c.getName(), "row-name"), spacer(),
                    label(c.getColorHex(), "cell-secondary", "num"), edit, remove);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("plain-row");
            list.getChildren().add(row);
        }
    }

    private void open(Category existing, TransactionType type) {
        new CategoryDialog(expenseList.getScene().getWindow(), existing, type).showAndWait().ifPresent(c -> {
            try {
                ctx.categories().save(c);
                ctx.dataChanged();
            } catch (RuntimeException ex) {
                inform("A category called \"" + c.getName() + "\" already exists.");
            }
        });
    }

    private void remove(Category c) {
        if (ctx.categories().isInUse(c.getId())) {
            inform("\"" + c.getName() + "\" is used by existing entries. Move or delete those entries first.");
            return;
        }
        ctx.categories().delete(c.getId());
        ctx.dataChanged();
    }

    private void inform(String message) {
        var a = new Alert(Alert.AlertType.INFORMATION, message);
        a.setHeaderText(null);
        a.initOwner(expenseList.getScene().getWindow());
        a.getDialogPane().getStylesheets().setAll(expenseList.getScene().getStylesheets());
        a.showAndWait();
    }
}
