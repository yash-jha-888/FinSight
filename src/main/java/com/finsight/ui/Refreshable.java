package com.finsight.ui;

/**
 * Anything on screen that can reload its data. The two tabs implement
 * this, so App can refresh all of them in one loop without knowing which is which.
 */
public interface Refreshable {

    void refresh();
}
