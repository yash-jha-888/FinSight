package com.finsight;

import com.finsight.db.DemoData;
import com.finsight.db.TransactionDao;
import com.finsight.ui.Refreshable;
import com.finsight.ui.SummaryTab;
import com.finsight.ui.TransactionsTab;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/** Starts the app: builds the window with a title bar and two tabs. */
public class App extends Application {

    private TransactionDao transactionDao = new TransactionDao();
    private Refreshable[] screens; // the two tabs

    @Override
    public void start(Stage stage) {
        DemoData.addIfEmpty(transactionDao);

        Label title = new Label("FinSight");
        title.getStyleClass().add("title");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setPadding(new Insets(12, 16, 12, 16));

        SummaryTab summaryTab = new SummaryTab(transactionDao);
        TransactionsTab transactionsTab = new TransactionsTab(this, transactionDao);
        screens = new Refreshable[]{summaryTab, transactionsTab};

        TabPane tabs = new TabPane(summaryTab, transactionsTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        BorderPane root = new BorderPane();
        root.setTop(title);
        root.setCenter(tabs);

        Scene scene = new Scene(root, 1000, 680);
        scene.getStylesheets().add(getClass().getResource("/com/finsight/style.css").toExternalForm());

        stage.setTitle("FinSight - Personal Finance Manager");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/finsight/icon.png")));
        stage.setScene(scene);
        stage.show();

        refreshAll();
    }

    // reloads both tabs; called after a transaction is added or deleted
    public void refreshAll() {
        for (Refreshable screen : screens) {
            screen.refresh(); // each tab runs its own version of refresh()
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
