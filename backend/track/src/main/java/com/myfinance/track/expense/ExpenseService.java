package com.myfinance.track.expense;

import com.myfinance.track.category.Category;
import com.myfinance.track.category.CategoryRepository;
import com.myfinance.track.common.PageResponse;
import com.myfinance.track.common.ResourceNotFoundException;
import com.myfinance.track.user.User;
import com.myfinance.track.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;

@Service
public class ExpenseService {

    private static final int MAX_PAGE_SIZE = 100;
    /** API name -> entity field. Anything not listed here is rejected. */
    private static final Map<String, String> SORT_FIELDS =
            Map.of("date", "expenseDate", "amount", "amount");

    private final ExpenseRepository expenses;
    private final CategoryRepository categories;
    private final UserRepository users;

    public ExpenseService(ExpenseRepository expenses, CategoryRepository categories, UserRepository users) {
        this.expenses = expenses;
        this.categories = categories;
        this.users = users;
    }

    @Transactional
    public ExpenseDto create(Long userId, ExpenseRequest req) {
        Category category = requireCategory(req.categoryId(), userId);
        User user = users.getReferenceById(userId);
        Expense saved = expenses.save(
                new Expense(user, category, req.amount(), clean(req.description()), req.date()));
        return ExpenseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseDto> list(Long userId, LocalDate from, LocalDate to, Long categoryId,
                                         int page, int size, String sort) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must not be after 'to'");
        }
        Pageable pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), buildSort(sort));
        return PageResponse.from(
                expenses.search(userId, from, to, categoryId, pageable).map(ExpenseDto::from));
    }

    @Transactional(readOnly = true)
    public ExpenseDto get(Long userId, Long id) {
        return ExpenseDto.from(findOwned(userId, id));
    }

    @Transactional
    public ExpenseDto update(Long userId, Long id, ExpenseRequest req) {
        Expense e = findOwned(userId, id);
        e.setCategory(requireCategory(req.categoryId(), userId));
        e.setAmount(req.amount());
        e.setDescription(clean(req.description()));
        e.setExpenseDate(req.date());
        return ExpenseDto.from(e);   // changes are saved automatically when the transaction commits
    }

    @Transactional
    public void delete(Long userId, Long id) {
        if (expenses.deleteByIdAndUserId(id, userId) == 0) {
            throw new ResourceNotFoundException("Expense not found");
        }
    }

    // ---- helpers ----

    private Expense findOwned(Long userId, Long id) {
        return expenses.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
    }

    private Category requireCategory(Long categoryId, Long userId) {
        return categories.findVisibleById(categoryId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown category"));
    }

    private static String clean(String description) {
        if (description == null) return null;
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Sort buildSort(String sort) {
        String[] parts = sort.split(",");
        String field = SORT_FIELDS.get(parts[0].trim());
        if (field == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported sort field. Use one of: " + SORT_FIELDS.keySet());
        }
        Sort.Direction dir = (parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        // "id" as a tie-breaker keeps pages stable when many expenses share a date
        return Sort.by(dir, field).and(Sort.by(Sort.Direction.DESC, "id"));
    }
}