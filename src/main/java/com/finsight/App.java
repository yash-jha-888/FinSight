package com.finsight;

import com.finsight.alert.AlertEngine;
import com.finsight.dao.BudgetRepository;
import com.finsight.dao.CategoryRepository;
import com.finsight.dao.Database;
import com.finsight.dao.TransactionRepository;
import com.finsight.service.DemoData;
import com.finsight.service.FinanceService;
import com.finsight.service.InsightService;
import com.finsight.ui.AppContext;
import com.finsight.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.Clock;

/**
 * Starts FinSight: opens the database, seeds demo data on first run, and shows the window.
 *
 * <p>Pass {@code --db=path/to/file.db} to use a different database file.
 */
public class App extends Application {

    private static final String[] FONTS = {
            "IBMPlexSans-Regular", "IBMPlexSans-Medium", "IBMPlexSans-SemiBold",
            "IBMPlexSerif-Light", "IBMPlexSerif-Regular", "IBMPlexSerif-LightItalic", "IBMPlexSerif-Medium",
            "IBMPlexMono-Regular", "IBMPlexMono-Medium",
    };

    private Database db;

    @Override
    public void start(Stage stage) {
        loadFonts();

        String dbArg = getParameters().getNamed().get("db");
        db = Database.open(dbArg != null ? Path.of(dbArg) : Database.defaultFile());

        var window = new MainWindow(createContext(db, Clock.systemDefaultZone()));

        stage.setTitle("FinSight");
        stage.setScene(window.createScene());
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        var icon = App.class.getResourceAsStream("/com/finsight/icon.png");
        if (icon != null) stage.getIcons().add(new Image(icon));
        stage.show();
    }

    public static void loadFonts() {
        for (String f : FONTS) {
            Font.loadFont(App.class.getResourceAsStream("/com/finsight/fonts/" + f + ".ttf"), 13);
        }
    }

    /** Wires repositories and services together, seeding demo data into an empty database. */
    public static AppContext createContext(Database db, Clock clock) {
        var categories = new CategoryRepository(db);
        var transactions = new TransactionRepository(db);
        var budgets = new BudgetRepository(db);
        var finance = new FinanceService(db, budgets, AlertEngine.withDefaultRules(), clock);
        var demo = new DemoData(db, categories, transactions, budgets);
        demo.ensureDefaultCategories();
        demo.seedIfEmpty(finance.today(), 6);
        return new AppContext(categories, transactions, budgets, finance, new InsightService(finance));
    }

    @Override
    public void stop() {
        if (db != null) db.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
