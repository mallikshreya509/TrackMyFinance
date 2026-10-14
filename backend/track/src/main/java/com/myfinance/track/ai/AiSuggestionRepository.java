package com.myfinance.track.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, Long> {
    Optional<AiSuggestion> findTopByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<AiSuggestion> findTopByUserIdAndDataHashOrderByCreatedAtDesc(Long userId, String dataHash);
    Optional<AiSuggestion> findTopByUserIdAndPeriodStartAndPeriodEndOrderByCreatedAtDesc(
        Long userId, java.time.LocalDate periodStart, java.time.LocalDate periodEnd);
}
