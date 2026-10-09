package com.myfinance.track.expense;

import com.myfinance.track.category.CategoryDto;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseDto(Long id, BigDecimal amount, CategoryDto category,
                         String description, LocalDate date) {

    public static ExpenseDto from(Expense e) {
        return new ExpenseDto(e.getId(), e.getAmount(), CategoryDto.from(e.getCategory()),
                e.getDescription(), e.getExpenseDate());
    }
}