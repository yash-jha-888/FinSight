package com.finsight.alert;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Runs every rule over every budget and returns the alerts, most urgent first. */
public class AlertEngine {

    private final List<AlertRule> rules;

    public AlertEngine(List<AlertRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static AlertEngine withDefaultRules() {
        return new AlertEngine(List.of(new OverspentRule(), new ThresholdRule(), new PaceRule()));
    }

    public List<Alert> evaluate(List<BudgetStatus> statuses) {
        return statuses.stream()
                .flatMap(status -> rules.stream().map(rule -> rule.evaluate(status)))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(Alert::severity).reversed())
                .toList();
    }
}
