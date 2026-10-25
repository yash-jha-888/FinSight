package com.finsight.service;

import com.finsight.model.TransactionType;
import com.finsight.service.Insight.Tone;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Turns the numbers from {@link FinanceService} into a few plain-language observations. */
public class InsightService {

    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.of("en", "IN"));

    private final FinanceService finance;

    public InsightService(FinanceService finance) {
        this.finance = finance;
    }

    public List<Insight> insightsFor(YearMonth month) {
        List<Insight> insights = new ArrayList<>();
        MonthlySummary current = finance.monthlySummary(month);
        MonthlySummary previous = finance.monthlySummary(month.minusMonths(1));

        if (current.transactionCount() == 0) {
            insights.add(new Insight(Tone.NEUTRAL, "Nothing recorded yet",
                    "Add a few transactions for " + month + " to see insights here."));
            return insights;
        }

        savingsRate(current).ifPresent(insights::add);
        spendingChange(current, previous).ifPresent(insights::add);
        topCategory(month).ifPresent(insights::add);
        biggestCategoryMove(month).ifPresent(insights::add);
        return insights;
    }

    private java.util.Optional<Insight> savingsRate(MonthlySummary m) {
        if (m.income().signum() == 0) {
            return java.util.Optional.empty();
        }
        int pct = (int) Math.round(m.savingsRate() * 100);
        if (pct >= 20) {
            return java.util.Optional.of(new Insight(Tone.POSITIVE, "You kept " + pct + "% of your income",
                    "That's " + MONEY.format(m.net()) + " saved. 20% or more is a healthy target."));
        }
        if (pct >= 0) {
            return java.util.Optional.of(new Insight(Tone.NEUTRAL, "You kept " + pct + "% of your income",
                    "Saving 20% would mean spending about "
                            + MONEY.format(m.income().multiply(new BigDecimal("0.8"))) + " a month."));
        }
        return java.util.Optional.of(new Insight(Tone.NEGATIVE, "You spent more than you earned",
                "Expenses were " + MONEY.format(m.net().negate()) + " above income this month."));
    }

    private java.util.Optional<Insight> spendingChange(MonthlySummary current, MonthlySummary previous) {
        if (previous.expense().signum() == 0) {
            return java.util.Optional.empty();
        }
        double change = current.expense().subtract(previous.expense())
                .divide(previous.expense(), 4, RoundingMode.HALF_UP).doubleValue();
        int pct = (int) Math.round(Math.abs(change) * 100);
        if (pct < 5) {
            return java.util.Optional.of(new Insight(Tone.NEUTRAL, "Spending is steady",
                    "Within 5% of last month's " + MONEY.format(previous.expense()) + "."));
        }
        boolean up = change > 0;
        return java.util.Optional.of(new Insight(up ? Tone.NEGATIVE : Tone.POSITIVE,
                "Spending is " + (up ? "up " : "down ") + pct + "% on last month",
                MONEY.format(current.expense()) + " so far, against " + MONEY.format(previous.expense()) + "."));
    }

    private java.util.Optional<Insight> topCategory(YearMonth month) {
        var totals = finance.categoryTotals(month, TransactionType.EXPENSE);
        if (totals.isEmpty()) {
            return java.util.Optional.empty();
        }
        BigDecimal all = totals.stream().map(CategoryTotal::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        var top = totals.get(0);
        int share = top.total().multiply(BigDecimal.valueOf(100)).divide(all, 0, RoundingMode.HALF_UP).intValue();
        return java.util.Optional.of(new Insight(Tone.NEUTRAL,
                top.category().getName() + " is your biggest expense",
                share + "% of spending, across " + top.transactionCount()
                        + (top.transactionCount() == 1 ? " transaction." : " transactions.")));
    }

    private java.util.Optional<Insight> biggestCategoryMove(YearMonth month) {
        var now = finance.categoryTotals(month, TransactionType.EXPENSE);
        var before = finance.categoryTotals(month.minusMonths(1), TransactionType.EXPENSE);
        CategoryTotal best = null;
        BigDecimal bestDelta = BigDecimal.ZERO;
        for (var c : now) {
            var prev = before.stream().filter(b -> b.category().equals(c.category())).findFirst();
            if (prev.isEmpty()) continue;
            BigDecimal delta = c.total().subtract(prev.get().total());
            if (delta.abs().compareTo(bestDelta.abs()) > 0) {
                best = c;
                bestDelta = delta;
            }
        }
        if (best == null || bestDelta.abs().compareTo(new BigDecimal("500")) < 0) {
            return java.util.Optional.empty();
        }
        boolean up = bestDelta.signum() > 0;
        return java.util.Optional.of(new Insight(up ? Tone.NEGATIVE : Tone.POSITIVE,
                best.category().getName() + (up ? " rose " : " fell ") + "by " + MONEY.format(bestDelta.abs()),
                "Compared with all of last month."));
    }
}
