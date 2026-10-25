package com.finsight.alert;

/** Fires once spending is over the limit. */
public class OverspentRule extends BudgetAlertRule {

    public OverspentRule() {
        super(Severity.CRITICAL);
    }

    @Override
    protected boolean appliesTo(BudgetStatus status) {
        return status.spent().compareTo(status.budget().getLimit()) > 0;
    }

    @Override
    protected String title(BudgetStatus status) {
        return status.budget().getCategory().getName() + " is over budget";
    }

    @Override
    protected String message(BudgetStatus status) {
        var over = status.spent().subtract(status.budget().getLimit());
        return "Spent " + status.spent() + " of " + status.budget().getLimit() + ", which is " + over + " over.";
    }
}
