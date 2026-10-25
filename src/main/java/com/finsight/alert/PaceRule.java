package com.finsight.alert;

/**
 * Fires when spending is running well ahead of the calendar, for example 60%
 * of the budget gone only a third of the way through the month.
 */
public class PaceRule extends BudgetAlertRule {

    /** How far ahead of the calendar spending must be before we mention it. */
    private static final double MARGIN = 0.15;

    public PaceRule() {
        super(Severity.INFO);
    }

    @Override
    protected boolean appliesTo(BudgetStatus status) {
        double used = status.usedFraction();
        double elapsed = status.monthElapsedFraction();
        return used < 0.8 && elapsed < 1 && used - elapsed > MARGIN;
    }

    @Override
    protected String title(BudgetStatus status) {
        return status.budget().getCategory().getName() + " is ahead of pace";
    }

    @Override
    protected String message(BudgetStatus status) {
        return percent(status.usedFraction()) + " used with " + percent(status.monthElapsedFraction())
                + " of the month gone.";
    }
}
