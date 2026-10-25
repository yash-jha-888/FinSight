package com.finsight.ui;

import com.finsight.dao.BudgetRepository;
import com.finsight.dao.CategoryRepository;
import com.finsight.dao.TransactionRepository;
import com.finsight.service.FinanceService;
import com.finsight.service.InsightService;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.time.YearMonth;

/**
 * Shared state for every screen: the services, the month being viewed, and a
 * counter that goes up whenever data changes so views know to reload.
 */
public class AppContext {

    private final CategoryRepository categories;
    private final TransactionRepository transactions;
    private final BudgetRepository budgets;
    private final FinanceService finance;
    private final InsightService insights;

    private final ObjectProperty<YearMonth> month;
    private final IntegerProperty revision = new SimpleIntegerProperty();

    public AppContext(CategoryRepository categories, TransactionRepository transactions, BudgetRepository budgets,
                      FinanceService finance, InsightService insights) {
        this.categories = categories;
        this.transactions = transactions;
        this.budgets = budgets;
        this.finance = finance;
        this.insights = insights;
        this.month = new SimpleObjectProperty<>(YearMonth.from(finance.today()));
    }

    public CategoryRepository categories() {
        return categories;
    }

    public TransactionRepository transactions() {
        return transactions;
    }

    public BudgetRepository budgets() {
        return budgets;
    }

    public FinanceService finance() {
        return finance;
    }

    public InsightService insights() {
        return insights;
    }

    public ObjectProperty<YearMonth> monthProperty() {
        return month;
    }

    public YearMonth month() {
        return month.get();
    }

    public IntegerProperty revisionProperty() {
        return revision;
    }

    /** Call after any insert, update or delete so every view refreshes. */
    public void dataChanged() {
        revision.set(revision.get() + 1);
    }
}
