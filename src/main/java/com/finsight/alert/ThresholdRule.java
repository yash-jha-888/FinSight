package com.finsight.alert;

/** Fires when spending passes a share of the limit (80% by default) but is not yet over. */
public class ThresholdRule extends BudgetAlertRule {

    private final double threshold;

    public ThresholdRule(double threshold) {
        super(Severity.WARNING);
        if (threshold <= 0 || threshold >= 1) {
            throw new IllegalArgumentException("Threshold must be between 0 and 1");
        }
        this.threshold = threshold;
    }

    public ThresholdRule() {
        this(0.8);
    }

    @Override
    protected boolean appliesTo(BudgetStatus status) {
        double used = status.usedFraction();
        return used >= threshold && used <= 1;
    }

    @Override
    protected String title(BudgetStatus status) {
        return status.budget().getCategory().getName() + " is at " + percent(status.usedFraction());
    }

    @Override
    protected String message(BudgetStatus status) {
        return money(status.budget().remaining(status.spent())) + " left for the rest of the month.";
    }
}
