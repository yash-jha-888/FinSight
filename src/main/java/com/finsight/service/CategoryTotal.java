package com.finsight.service;

import com.finsight.model.Category;

import java.math.BigDecimal;

/** How much went to (or came from) one category in a period. */
public record CategoryTotal(Category category, BigDecimal total, int transactionCount) {
}
