package com.myfinance.track.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** System defaults (user is null) plus this user's own categories. */
    @Query("""
           select c from Category c
           where c.user is null or c.user.id = :userId
           order by c.name
           """)
    List<Category> findVisibleToUser(@Param("userId") Long userId);

    @Query("""
           select c from Category c
           where c.id = :id and (c.user is null or c.user.id = :userId)
           """)
    Optional<Category> findVisibleById(@Param("id") Long id, @Param("userId") Long userId);
}