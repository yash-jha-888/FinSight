package com.finsight.alert;

import com.finsight.model.Category;

/** A message shown to the user about one budget. Immutable. */
public record Alert(Severity severity, Category category, String title, String message) {
}
