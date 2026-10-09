package com.myfinance.track.dashboard;

import java.math.BigDecimal;

/** Query result: one row per category. */
public record CategoryTotal(Long categoryId, String name, String color, BigDecimal total) {}