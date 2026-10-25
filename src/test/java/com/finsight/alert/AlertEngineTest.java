package com.finsight.alert;

import com.finsight.model.Budget;
import com.finsight.model.Category;
import com.finsight.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlertEngineTest {

    private final Category food = new Category(1, "Food", TransactionType.EXPENSE, "#B5523B");
    private final Budget budget = new Budget(food, YearMonth.of(2026, 10), new BigDecimal("1000"));
    private final AlertEngine engine = AlertEngine.withDefaultRules();

    private List<Alert> alertsFor(String spent, int dayOfMonth) {
        var status = new BudgetStatus(budget, new BigDecimal(spent), LocalDate.of(2026, 10, dayOfMonth));
        return engine.evaluate(List.of(status));
    }

    @Test
    void noAlertWhenComfortablyUnder() {
        assertTrue(alertsFor("300", 20).isEmpty());
    }

    @Test
    void warningPastEightyPercent() {
        var alerts = alertsFor("850", 25);
        assertEquals(1, alerts.size());
        assertEquals(Severity.WARNING, alerts.get(0).severity());
    }

    @Test
    void criticalWhenOverspent() {
        var alerts = alertsFor("1250", 25);
        assertEquals(1, alerts.size());
        assertEquals(Severity.CRITICAL, alerts.get(0).severity());
        assertTrue(alerts.get(0).message().contains("₹250 over"));
    }

    @Test
    void infoWhenSpendingAheadOfCalendar() {
        var alerts = alertsFor("600", 5);
        assertEquals(1, alerts.size());
        assertEquals(Severity.INFO, alerts.get(0).severity());
    }

    @Test
    void mostUrgentFirst() {
        var other = new Budget(new Category(2, "Fun", TransactionType.EXPENSE, "#3B5BB5"),
                YearMonth.of(2026, 10), new BigDecimal("100"));
        var today = LocalDate.of(2026, 10, 25);
        var alerts = engine.evaluate(List.of(
                new BudgetStatus(budget, new BigDecimal("850"), today),
                new BudgetStatus(other, new BigDecimal("200"), today)));
        assertEquals(List.of(Severity.CRITICAL, Severity.WARNING), alerts.stream().map(Alert::severity).toList());
    }
}
