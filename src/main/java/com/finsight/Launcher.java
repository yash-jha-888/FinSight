package com.finsight;

/**
 * Plain entry point for running from a fat jar or an IDE without JavaFX on
 * the module path. JavaFX refuses to start if the main class itself extends
 * {@code Application}, so this class just hands over to {@link App}.
 */
public final class Launcher {

    public static void main(String[] args) {
        App.main(args);
    }
}
