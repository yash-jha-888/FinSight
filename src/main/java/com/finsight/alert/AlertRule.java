package com.finsight.alert;

import java.util.Optional;

/**
 * One check that may produce an alert for a budget.
 *
 * <p><b>OOP: interface and polymorphism.</b> The {@link AlertEngine} holds a
 * list of {@code AlertRule}s and calls {@link #evaluate} on each without
 * knowing which concrete rule it is. Adding a new kind of alert means adding
 * a class, not editing the engine.
 */
public interface AlertRule {

    Optional<Alert> evaluate(BudgetStatus status);
}
