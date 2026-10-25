package com.finsight.alert;

import com.finsight.model.Budget;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Everything a rule needs to judge one budget: the budget, what has been
 * spent against it, and today's date (passed in so tests can fix it).
 */
public record BudgetStatus(Budget budget, BigDecimal spent, LocalDate today) {

    public double usedFraction() {
        return budget.usedFraction(spent);
    }

    /** Share of the budget month that has passed, from 0 to 1. */
    public double monthElapsedFraction() {
        var month = budget.getMonth();
        var current = java.time.YearMonth.from(today);
        if (current.isBefore(month)) return 0;
        if (current.isAfter(month)) return 1;
        return (double) today.getDayOfMonth() / month.lengthOfMonth();
    }
}
