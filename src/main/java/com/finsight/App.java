package com.finsight;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Entry point. The real interface replaces this placeholder in step 3. */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        var root = new StackPane(new Label("FinSight"));
        stage.setTitle("FinSight");
        stage.setScene(new Scene(root, 1200, 760));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
