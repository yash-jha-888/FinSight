package com.finsight.ui.view;

import com.finsight.model.Expense;
import com.finsight.model.Income;
import com.finsight.model.Transaction;
import com.finsight.model.TransactionType;
import com.finsight.ui.AppContext;
import com.finsight.ui.Fmt;
import com.finsight.ui.dialog.TransactionDialog;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/** Every transaction in the selected month, with search, filtering and editing. */
public class LedgerView extends View {

    private final FilteredList<Transaction> rows = new FilteredList<>(FXCollections.observableArrayList());
    private final TableView<Transaction> table = new TableView<>(rows);
    private final TextField search = new TextField();
    private final ToggleGroup filter = new ToggleGroup();
    private final Label totals = label("", "ledger-totals");

    public LedgerView(AppContext ctx) {
        super(ctx);
    }

    @Override
    public String getTitle() {
        return "Ledger";
    }

    @Override
    public String getSubtitle() {
        return "Every entry, newest first";
    }

    @Override
    protected Node build() {
        search.setPromptText("Search descriptions and categories");
        search.getStyleClass().add("search");
        search.textProperty().addListener((obs, o, n) -> applyFilter());
        HBox.setHgrow(search, Priority.ALWAYS);
        search.setMaxWidth(380);

        var all = toggle("All", null);
        var inc = toggle("Income", TransactionType.INCOME);
        var exp = toggle("Expenses", TransactionType.EXPENSE);
        all.setSelected(true);
        filter.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == null) o.setSelected(true);
            applyFilter();
        });
        exp.getStyleClass().add("last");
        var seg = new HBox(all, inc, exp);
        seg.getStyleClass().add("segmented");

        var toolbar = new HBox(16, search, seg, spacer(), totals);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");

        buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        var hint = label("Double-click a row to edit. Press Delete to remove it.", "hint");
        var box = new VBox(20, toolbar, table, hint);
        box.getStyleClass().addAll("page", "page-fill");
        return box;
    }

    private ToggleButton toggle(String text, TransactionType type) {
        var b = new ToggleButton(text);
        b.setUserData(type);
        b.setToggleGroup(filter);
        return b;
    }

    private void buildTable() {
        table.getStyleClass().add("ledger");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(label("No entries for this month yet.", "muted"));

        var date = new TableColumn<Transaction, LocalDate>("Date");
        date.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getDate()));
        date.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                if (empty || d == null) {
                    setGraphic(null);
                    return;
                }
                var day = label(Fmt.day(d), "cell-primary");
                var wd = label(Fmt.weekday(d), "cell-secondary");
                var box = new HBox(8, day, wd);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        date.setPrefWidth(120);
        date.setMinWidth(110);

        var desc = new TableColumn<Transaction, Transaction>("Description");
        desc.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        desc.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Transaction t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) {
                    setGraphic(null);
                    return;
                }
                var text = t.getDescription().isBlank() ? t.getCategory().getName() : t.getDescription();
                setGraphic(label(text, "cell-primary"));
            }
        });
        desc.setPrefWidth(320);

        var cat = new TableColumn<Transaction, Transaction>("Category");
        cat.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        cat.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Transaction t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) {
                    setGraphic(null);
                    return;
                }
                var dot = new Circle(4);
                dot.setStyle("-fx-fill: " + t.getCategory().getColorHex() + ";");
                var box = new HBox(8, dot, label(t.getCategory().getName(), "cell-primary"));
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        cat.setPrefWidth(170);

        var via = new TableColumn<Transaction, String>("Via");
        via.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(switch (c.getValue()) {
            case Expense e -> e.getPaymentMethod().getLabel();
            case Income i -> i.getSource();
            default -> "";
        }));
        via.setPrefWidth(130);
        via.getStyleClass().add("muted-column");

        var amount = new TableColumn<Transaction, Transaction>("Amount");
        amount.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        amount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Transaction t, boolean empty) {
                super.updateItem(t, empty);
                getStyleClass().removeAll("positive", "negative");
                if (empty || t == null) {
                    setText(null);
                    return;
                }
                // Polymorphism: getSignedAmount() already knows which way the money moved.
                setText(Fmt.signed(t.getSignedAmount()));
                getStyleClass().add(t.getSignedAmount().signum() > 0 ? "positive" : "negative");
            }
        });
        amount.getStyleClass().add("amount-column");
        amount.setPrefWidth(150);
        amount.setMinWidth(130);

        table.getColumns().setAll(java.util.List.of(date, desc, cat, via, amount));

        table.setRowFactory(tv -> {
            var row = new TableRow<Transaction>();
            var edit = new MenuItem("Edit");
            edit.setOnAction(e -> edit(row.getItem()));
            var delete = new MenuItem("Delete");
            delete.setOnAction(e -> delete(row.getItem()));
            var menu = new ContextMenu(edit, delete);
            row.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(row.emptyProperty())
                    .then((ContextMenu) null).otherwise(menu));
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) edit(row.getItem());
            });
            return row;
        });
        table.setOnKeyPressed(e -> {
            var selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) delete(selected);
            if (e.getCode() == KeyCode.ENTER) edit(selected);
        });
    }

    @Override
    protected void reload() {
        YearMonth month = ctx.month();
        ((javafx.collections.ObservableList<Transaction>) rows.getSource())
                .setAll(ctx.transactions().findByMonth(month));
        applyFilter();
    }

    private void applyFilter() {
        String q = search.getText() == null ? "" : search.getText().strip().toLowerCase();
        var type = filter.getSelectedToggle() == null ? null : (TransactionType) filter.getSelectedToggle().getUserData();
        rows.setPredicate(t -> (type == null || t.getType() == type)
                && (q.isEmpty() || t.getDescription().toLowerCase().contains(q)
                || t.getCategory().getName().toLowerCase().contains(q)));

        BigDecimal in = BigDecimal.ZERO, out = BigDecimal.ZERO;
        for (var t : rows) {
            if (t.getType() == TransactionType.INCOME) in = in.add(t.getAmount());
            else out = out.add(t.getAmount());
        }
        totals.setText(rows.size() + " entries   ·   in " + Fmt.moneyWhole(in) + "   ·   out " + Fmt.moneyWhole(out));
    }

    private void edit(Transaction t) {
        var dialog = new TransactionDialog(table.getScene().getWindow(), ctx.categories().findAll(), t, t.getDate());
        dialog.showAndWait().ifPresent(updated -> {
            ctx.transactions().save(updated);
            ctx.dataChanged();
        });
    }

    private void delete(Transaction t) {
        var confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete " + Fmt.money(t.getAmount()) + " " + t.getCategory().getName().toLowerCase()
                        + " on " + Fmt.day(t.getDate()) + "?", ButtonType.CANCEL, ButtonType.OK);
        confirm.setHeaderText(null);
        confirm.initOwner(table.getScene().getWindow());
        confirm.getDialogPane().getStylesheets().setAll(table.getScene().getStylesheets());
        confirm.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> {
            ctx.transactions().delete(t.getId());
            ctx.dataChanged();
        });
    }

    /** Opens the new-entry dialog. Called from the header button and Ctrl+N. */
    public static void addNew(AppContext ctx, javafx.stage.Window owner) {
        var today = ctx.finance().today();
        var month = ctx.month();
        var defaultDate = month.equals(YearMonth.from(today)) ? today : month.atEndOfMonth();
        new TransactionDialog(owner, ctx.categories().findAll(), null, defaultDate)
                .showAndWait().ifPresent(t -> {
                    ctx.transactions().save(t);
                    ctx.dataChanged();
                });
    }
}
