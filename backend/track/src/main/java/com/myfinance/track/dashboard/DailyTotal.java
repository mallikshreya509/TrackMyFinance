package com.myfinance.track.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyTotal(LocalDate date, BigDecimal total) {}