package com.finsight.alert;

import java.util.Optional;

/**
 * Base class for rules that fire on a condition and then describe it.
 *
 * <p><b>OOP: abstraction (template method).</b> {@link #evaluate} fixes the
 * order of steps; subclasses fill in only the condition and the wording.
 */
public abstract class BudgetAlertRule implements AlertRule {

    private final Severity severity;

    protected BudgetAlertRule(Severity severity) {
        this.severity = severity;
    }

    @Override
    public final Optional<Alert> evaluate(BudgetStatus status) {
        if (!appliesTo(status)) {
            return Optional.empty();
        }
        var category = status.budget().getCategory();
        return Optional.of(new Alert(severity, category, title(status), message(status)));
    }

    protected abstract boolean appliesTo(BudgetStatus status);

    protected abstract String title(BudgetStatus status);

    protected abstract String message(BudgetStatus status);

    protected static String money(java.math.BigDecimal amount) {
        var f = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.of("en", "IN"));
        f.setMaximumFractionDigits(0);
        return f.format(amount.setScale(0, java.math.RoundingMode.HALF_UP));
    }

    protected static String percent(double fraction) {
        return Math.round(fraction * 100) + "%";
    }
}
