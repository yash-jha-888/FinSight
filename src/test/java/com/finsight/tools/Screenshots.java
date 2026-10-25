package com.finsight.tools;

import com.finsight.App;
import com.finsight.dao.Database;
import com.finsight.ui.MainWindow;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Developer tool that renders every screen in light and dark mode to PNGs in
 * docs/screenshots, using a fresh demo database. Not part of the app.
 *
 * <pre>mvn -q test-compile exec:java -Dexec.mainClass=com.finsight.tools.ScreenshotLauncher -Dexec.classpathScope=test</pre>
 */
public class Screenshots extends Application {

    private static final String[] NAMES = {"overview", "ledger", "budgets", "insights", "categories"};

    @Override
    public void start(Stage stage) throws Exception {
        App.loadFonts();
        Path tmp = Files.createTempFile("finsight-shots", ".db");
        Files.delete(tmp);
        var db = Database.open(tmp);
        var window = new MainWindow(App.createContext(db, java.time.Clock.systemDefaultZone()));
        Scene scene = window.createScene();
        stage.setScene(scene);
        stage.show();

        File out = new File("docs/screenshots");
        out.mkdirs();

        Deque<Runnable> steps = new ArrayDeque<>();
        for (boolean dark : new boolean[]{false, true}) {
            for (int i = 0; i < NAMES.length; i++) {
                int index = i;
                steps.add(() -> {
                    window.setDark(dark);
                    window.showView(index);
                });
                steps.add(() -> save(scene, new File(out, NAMES[index] + (dark ? "-dark" : "") + ".png")));
            }
        }
        // One tall shot of the whole overview page for the README.
        steps.add(() -> {
            window.setDark(false);
            window.showView(0);
            stage.setHeight(1340);
        });
        steps.add(() -> save(scene, new File(out, "overview-full.png")));
        var dialog = new javafx.scene.control.Dialog<?>[1];
        steps.add(() -> {
            stage.setHeight(840);
            var ctx = window.context();
            var d = new com.finsight.ui.dialog.TransactionDialog(stage, ctx.categories().findAll(), null,
                    ctx.finance().today());
            dialog[0] = d;
            d.show();
        });
        steps.add(() -> {
            save(dialog[0].getDialogPane().getScene(), new File(out, "dialog.png"));
            dialog[0].close();
        });
        steps.add(() -> {
            db.close();
            Platform.exit();
        });
        runNext(steps);
    }

    private static void runNext(Deque<Runnable> steps) {
        if (steps.isEmpty()) return;
        var pause = new PauseTransition(Duration.millis(400));
        pause.setOnFinished(e -> {
            steps.poll().run();
            runNext(steps);
        });
        pause.play();
    }

    private static void save(Scene scene, File file) {
        WritableImage img = scene.snapshot(null);
        int w = (int) img.getWidth(), h = (int) img.getHeight();
        var buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        var reader = img.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                buf.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        try {
            ImageIO.write(buf, "png", file);
            System.out.println("Saved " + file);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
