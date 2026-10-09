package com.myfinance.track.expense;

import com.myfinance.track.common.PageResponse;
import com.myfinance.track.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final ExpenseService service;
    private final CurrentUser currentUser;

    public ExpenseController(ExpenseService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseDto create(@Valid @RequestBody ExpenseRequest request) {
        return service.create(currentUser.id(), request);
    }

    @GetMapping
    public PageResponse<ExpenseDto> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "date,desc") String sort) {
        return service.list(currentUser.id(), from, to, categoryId, page, size, sort);
    }

    @GetMapping("/{id}")
    public ExpenseDto get(@PathVariable Long id) {
        return service.get(currentUser.id(), id);
    }

    @PutMapping("/{id}")
    public ExpenseDto update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        return service.update(currentUser.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(currentUser.id(), id);
    }
}