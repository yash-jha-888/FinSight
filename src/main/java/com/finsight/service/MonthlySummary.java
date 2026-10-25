package com.finsight.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

/** Income, spending and what is left for one month. */
public record MonthlySummary(YearMonth month, BigDecimal income, BigDecimal expense, int transactionCount) {

    public static MonthlySummary empty(YearMonth month) {
        return new MonthlySummary(month, BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), 0);
    }

    public BigDecimal net() {
        return income.subtract(expense);
    }

    /** Share of income that was not spent, from 0 to 1. Zero when there was no income. */
    public double savingsRate() {
        if (income.signum() == 0) {
            return 0;
        }
        return net().divide(income, 4, RoundingMode.HALF_UP).doubleValue();
    }
}
