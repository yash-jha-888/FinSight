package com.finsight.dao;

import com.finsight.model.Budget;
import com.finsight.model.Category;
import com.finsight.model.Expense;
import com.finsight.model.Income;
import com.finsight.model.PaymentMethod;
import com.finsight.model.TransactionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryTest {

    private Database db;
    private CategoryRepository categories;
    private TransactionRepository transactions;
    private BudgetRepository budgets;
    private Category salary;
    private Category food;

    @BeforeEach
    void setUp() {
        db = Database.inMemory();
        categories = new CategoryRepository(db);
        transactions = new TransactionRepository(db);
        budgets = new BudgetRepository(db);
        salary = categories.save(new Category("Salary", TransactionType.INCOME, "#3F6E57"));
        food = categories.save(new Category("Food", TransactionType.EXPENSE, "#B4653E"));
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void savesAndReadsBackBothSubclasses() {
        var pay = transactions.save(new Income(0, new BigDecimal("50000"), LocalDate.of(2026, 10, 1), salary,
                "Pay", "Employer"));
        var lunch = transactions.save(new Expense(new BigDecimal("249.99"), LocalDate.of(2026, 10, 2), food,
                "Lunch", PaymentMethod.UPI));

        var loadedPay = transactions.findById(pay.getId()).orElseThrow();
        var loadedLunch = transactions.findById(lunch.getId()).orElseThrow();

        assertInstanceOf(Income.class, loadedPay);
        assertEquals("Employer", ((Income) loadedPay).getSource());
        assertInstanceOf(Expense.class, loadedLunch);
        assertEquals(PaymentMethod.UPI, ((Expense) loadedLunch).getPaymentMethod());
        assertEquals(new BigDecimal("249.99"), loadedLunch.getAmount());
        assertEquals(2, transactions.findByMonth(YearMonth.of(2026, 10)).size());
    }

    @Test
    void updateCanChangeAmountAndCategory() {
        var e = transactions.save(new Expense(BigDecimal.TEN, LocalDate.of(2026, 10, 2), food, "x", PaymentMethod.CASH));
        e.setAmount(new BigDecimal("99"));
        e.setDescription("Changed");
        transactions.save(e);

        var loaded = transactions.findById(e.getId()).orElseThrow();
        assertEquals(new BigDecimal("99.00"), loaded.getAmount());
        assertEquals("Changed", loaded.getDescription());
    }

    @Test
    void deleteCascadesToDetailRow() throws Exception {
        var e = transactions.save(new Expense(BigDecimal.TEN, LocalDate.of(2026, 10, 2), food, "", PaymentMethod.CASH));
        assertTrue(transactions.delete(e.getId()));
        try (var st = db.connection().createStatement();
             var rs = st.executeQuery("SELECT COUNT(*) FROM expense_detail")) {
            rs.next();
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    void categoryInUseCannotBeDeleted() {
        transactions.save(new Expense(BigDecimal.TEN, LocalDate.of(2026, 10, 2), food, "", PaymentMethod.CASH));
        assertTrue(categories.isInUse(food.getId()));
        assertThrows(DataAccessException.class, () -> categories.delete(food.getId()));
    }

    @Test
    void duplicateCategoryNameIsRejectedIgnoringCase() {
        assertThrows(DataAccessException.class,
                () -> categories.save(new Category("FOOD", TransactionType.EXPENSE, "#000000")));
    }

    @Test
    void databaseTriggerBlocksMismatchedTypes() throws Exception {
        try (var st = db.connection().createStatement()) {
            var ex = assertThrows(java.sql.SQLException.class, () -> st.executeUpdate(
                    "INSERT INTO txn (type, amount_cents, txn_date, category_id) VALUES ('EXPENSE', 100, '2026-10-01', "
                            + salary.getId() + ")"));
            assertTrue(ex.getMessage().contains("does not match"));
        }
    }

    @Test
    void budgetSpendingComesFromTheView() {
        var oct = YearMonth.of(2026, 10);
        budgets.save(new Budget(food, oct, new BigDecimal("1000")));
        transactions.save(new Expense(new BigDecimal("300"), LocalDate.of(2026, 10, 5), food, "", PaymentMethod.UPI));
        transactions.save(new Expense(new BigDecimal("200"), LocalDate.of(2026, 10, 9), food, "", PaymentMethod.UPI));
        transactions.save(new Expense(new BigDecimal("999"), LocalDate.of(2026, 9, 9), food, "", PaymentMethod.UPI));

        var status = budgets.findWithSpending(oct);
        assertEquals(1, status.size());
        assertEquals(new BigDecimal("500.00"), status.get(0).spent());
    }

    @Test
    void savingBudgetTwiceReplacesTheLimit() {
        var oct = YearMonth.of(2026, 10);
        budgets.save(new Budget(food, oct, new BigDecimal("1000")));
        budgets.save(new Budget(food, oct, new BigDecimal("1500")));
        var all = budgets.findByMonth(oct);
        assertEquals(1, all.size());
        assertEquals(new BigDecimal("1500.00"), all.get(0).getLimit());
    }

    @Test
    void copyMonthCarriesBudgetsForward() {
        budgets.save(new Budget(food, YearMonth.of(2026, 9), new BigDecimal("800")));
        assertEquals(1, budgets.copyMonth(YearMonth.of(2026, 9), YearMonth.of(2026, 10)));
        assertEquals(1, budgets.findByMonth(YearMonth.of(2026, 10)).size());
    }

    @Test
    void failedTransactionRollsBack() {
        assertThrows(DataAccessException.class, () -> db.inTransaction(conn -> {
            categories.save(new Category("Temp", TransactionType.EXPENSE, "#111111"));
            throw new java.sql.SQLException("boom");
        }));
        assertTrue(categories.findAll().stream().noneMatch(c -> c.getName().equals("Temp")));
    }

    @Test
    void scriptSplitterKeepsTriggersWhole() {
        var parts = Database.splitStatements("""
                CREATE TABLE a (x);
                CREATE TRIGGER t BEFORE INSERT ON a
                BEGIN
                    SELECT 1;
                END;
                """);
        assertEquals(2, parts.size());
        assertTrue(parts.get(1).endsWith("END;"));
    }
}
