package com.myfinance.track.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /** Ownership is part of the query: someone else's id simply isn't found. */
    @EntityGraph(attributePaths = "category")
    Optional<Expense> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "category")
    @Query("""
           select e from Expense e
           where e.user.id = :userId
             and (:fromDate is null or e.expenseDate >= :fromDate)
             and (:toDate is null or e.expenseDate <= :toDate)
             and (:categoryId is null or e.category.id = :categoryId)
           """)
    Page<Expense> search(@Param("userId") Long userId,
                         @Param("fromDate") LocalDate fromDate,
                         @Param("toDate") LocalDate toDate,
                         @Param("categoryId") Long categoryId,
                         Pageable pageable);

    long deleteByIdAndUserId(Long id, Long userId);
}