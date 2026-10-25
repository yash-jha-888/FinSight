package com.finsight.service;

import com.finsight.alert.AlertEngine;
import com.finsight.dao.BudgetRepository;
import com.finsight.dao.CategoryRepository;
import com.finsight.dao.Database;
import com.finsight.dao.TransactionRepository;
import com.finsight.model.TransactionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class FinanceServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 25);

    private Database db;
    private TransactionRepository transactions;
    private FinanceService finance;

    @BeforeEach
    void setUp() {
        db = Database.inMemory();
        var categories = new CategoryRepository(db);
        transactions = new TransactionRepository(db);
        var budgets = new BudgetRepository(db);
        var clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        finance = new FinanceService(db, budgets, AlertEngine.withDefaultRules(), clock);
        new DemoData(db, categories, transactions, budgets).seedIfEmpty(TODAY, 4);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void demoDataCoversFourMonths() {
        var trend = finance.monthlyTrend(YearMonth.from(TODAY), 4);
        assertEquals(4, trend.size());
        assertTrue(trend.stream().allMatch(m -> m.transactionCount() > 20));
        assertTrue(transactions.findByMonth(YearMonth.from(TODAY)).stream()
                .noneMatch(t -> t.getDate().isAfter(TODAY)));
    }

    @Test
    void seedingTwiceDoesNothing() {
        int before = transactions.findAll().size();
        new DemoData(db, new CategoryRepository(db), transactions, new BudgetRepository(db)).seedIfEmpty(TODAY, 4);
        assertEquals(before, transactions.findAll().size());
    }

    @Test
    void summaryMatchesSumOfTransactions() {
        var month = YearMonth.from(TODAY);
        var summary = finance.monthlySummary(month);
        BigDecimal net = transactions.findByMonth(month).stream()
                .map(t -> t.getSignedAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(net, summary.net());
    }

    @Test
    void categoryTotalsAddUpToMonthlyExpense() {
        var month = YearMonth.from(TODAY);
        BigDecimal sum = finance.categoryTotals(month, TransactionType.EXPENSE).stream()
                .map(CategoryTotal::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(finance.monthlySummary(month).expense(), sum);
    }

    @Test
    void dailySpendingAddsUpToMonthlyExpense() {
        var month = YearMonth.from(TODAY);
        BigDecimal sum = java.util.Arrays.stream(finance.dailySpending(month)).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(finance.monthlySummary(month).expense(), sum);
    }

    @Test
    void emptyMonthGivesZeroSummary() {
        var summary = finance.monthlySummary(YearMonth.of(2020, 1));
        assertEquals(0, summary.transactionCount());
        assertEquals(0, summary.savingsRate());
    }

    @Test
    void insightsAreProducedForDemoMonth() {
        var insights = new InsightService(finance).insightsFor(YearMonth.from(TODAY));
        assertFalse(insights.isEmpty());
        insights.forEach(i -> assertFalse(i.headline().isBlank()));
    }

    @Test
    void budgetStatusesExistForCurrentMonth() {
        assertEquals(5, finance.budgetStatuses(YearMonth.from(TODAY)).size());
    }
}
