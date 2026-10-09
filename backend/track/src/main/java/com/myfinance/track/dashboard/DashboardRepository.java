package com.myfinance.track.dashboard;

import com.myfinance.track.expense.Expense;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Read-only aggregate queries. Every query is scoped to one user. */
public interface DashboardRepository extends Repository<Expense, Long> {

    /** Returns null when there are no rows; the service turns that into zero. */
    @Query("""
           select sum(e.amount) from Expense e
           where e.user.id = :userId and e.expenseDate between :fromDate and :toDate
           """)
    BigDecimal sumAmount(@Param("userId") Long userId,
                         @Param("fromDate") LocalDate fromDate,
                         @Param("toDate") LocalDate toDate);

    @Query("""
           select count(e) from Expense e
           where e.user.id = :userId and e.expenseDate between :fromDate and :toDate
           """)
    long countExpenses(@Param("userId") Long userId,
                       @Param("fromDate") LocalDate fromDate,
                       @Param("toDate") LocalDate toDate);

    @Query("""
           select new com.myfinance.track.dashboard.CategoryTotal(c.id, c.name, c.color, sum(e.amount))
           from Expense e join e.category c
           where e.user.id = :userId and e.expenseDate between :fromDate and :toDate
           group by c.id, c.name, c.color
           order by sum(e.amount) desc
           """)
    List<CategoryTotal> totalsByCategory(@Param("userId") Long userId,
                                         @Param("fromDate") LocalDate fromDate,
                                         @Param("toDate") LocalDate toDate);

    @Query("""
           select new com.myfinance.track.dashboard.DailyTotal(e.expenseDate, sum(e.amount))
           from Expense e
           where e.user.id = :userId and e.expenseDate between :fromDate and :toDate
           group by e.expenseDate
           order by e.expenseDate
           """)
    List<DailyTotal> totalsByDay(@Param("userId") Long userId,
                                 @Param("fromDate") LocalDate fromDate,
                                 @Param("toDate") LocalDate toDate);
}