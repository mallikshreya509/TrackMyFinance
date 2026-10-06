package com.myfinance.track;

import com.myfinance.track.ai.*;
import com.myfinance.track.category.Category;
import com.myfinance.track.category.CategoryRepository;
import com.myfinance.track.expense.Expense;
import com.myfinance.track.expense.ExpenseRepository;
import com.myfinance.track.user.User;
import com.myfinance.track.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.test.database.replace=none")
@Testcontainers
class RepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired ExpenseRepository expenses;
    @Autowired AiSuggestionRepository aiSuggestions;

    private User newUser(String email) {
        return users.save(new User(email, "not-a-real-hash", "Test"));
    }

    private Category anyCategory(User u) {
        return categories.findVisibleToUser(u.getId()).get(0);
    }

    @Test
    void flywaySeededEightDefaultCategories() {
        User u = newUser("a@example.com");
        assertThat(categories.findVisibleToUser(u.getId())).hasSize(8);
    }

    @Test
    void userCanOnlyReadTheirOwnExpense() {
        User a = newUser("a@example.com");
        User b = newUser("b@example.com");
        Expense e = expenses.saveAndFlush(new Expense(a, anyCategory(a),
                new BigDecimal("450.00"), "Lunch", LocalDate.of(2026, 9, 28)));

        assertThat(expenses.findByIdAndUserId(e.getId(), a.getId())).isPresent();
        assertThat(expenses.findByIdAndUserId(e.getId(), b.getId())).isEmpty();
    }

    @Test
    void searchFiltersByDateRangeAndCategory() {
        User u = newUser("a@example.com");
        var cats = categories.findVisibleToUser(u.getId());
        Category c1 = cats.get(0), c2 = cats.get(1);

        expenses.save(new Expense(u, c1, new BigDecimal("10.00"), "in range c1", LocalDate.of(2026, 9, 10)));
        expenses.save(new Expense(u, c2, new BigDecimal("20.00"), "in range c2", LocalDate.of(2026, 9, 11)));
        expenses.save(new Expense(u, c1, new BigDecimal("30.00"), "too early",  LocalDate.of(2026, 8, 1)));
        expenses.flush();

        var page = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "expenseDate"));

        Page<Expense> inRange = expenses.search(u.getId(),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null, page);
        assertThat(inRange.getTotalElements()).isEqualTo(2);

        Page<Expense> onlyC1 = expenses.search(u.getId(),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), c1.getId(), page);
        assertThat(onlyC1.getTotalElements()).isEqualTo(1);
        assertThat(onlyC1.getContent().get(0).getDescription()).isEqualTo("in range c1");
    }

    @Test
    void databaseRejectsZeroAmount() {
        User u = newUser("a@example.com");
        Expense bad = new Expense(u, anyCategory(u), BigDecimal.ZERO, "bad", LocalDate.of(2026, 9, 1));
        assertThatThrownBy(() -> expenses.saveAndFlush(bad))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aiSuggestionJsonRoundTrips() {
        User u = newUser("a@example.com");
        aiSuggestions.saveAndFlush(new AiSuggestion(u,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                "a".repeat(64), "{\"summary\":\"hello\"}", AiSource.FALLBACK, null));

        var latest = aiSuggestions.findTopByUserIdOrderByCreatedAtDesc(u.getId());
        assertThat(latest).isPresent();
        assertThat(latest.get().getResponseJson()).contains("summary");
        assertThat(latest.get().getSource()).isEqualTo(AiSource.FALLBACK);
    }
}