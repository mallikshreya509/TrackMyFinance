package com.myfinance.track.ai;

import com.myfinance.track.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class AiSuggestionStore {

    public record StoredInsight(InsightResult result, AiSource source, Instant createdAt,
                                LocalDate from, LocalDate to) {}

    private static final Logger log = LoggerFactory.getLogger(AiSuggestionStore.class);

    private final AiSuggestionRepository repo;
    private final UserRepository users;
    private final JsonMapper mapper;

    public AiSuggestionStore(AiSuggestionRepository repo, UserRepository users, JsonMapper mapper) {
        this.repo = repo;
        this.users = users;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Optional<StoredInsight> findByHash(Long userId, String hash) {
        return repo.findTopByUserIdAndDataHashOrderByCreatedAtDesc(userId, hash).flatMap(this::toStored);
    }

    @Transactional(readOnly = true)
    public Optional<StoredInsight> findLatest(Long userId) {
        return repo.findTopByUserIdOrderByCreatedAtDesc(userId).flatMap(this::toStored);
    }

    @Transactional(readOnly = true)
    public Optional<StoredInsight> findLatestForPeriod(Long userId, LocalDate from, LocalDate to) {
        return repo.findTopByUserIdAndPeriodStartAndPeriodEndOrderByCreatedAtDesc(userId, from, to)
                .flatMap(this::toStored);
    }

    @Transactional
    public StoredInsight save(Long userId, LocalDate from, LocalDate to, String hash,
                              InsightResult result, AiSource source) {
        String json = mapper.writeValueAsString(result);
        AiSuggestion saved = repo.save(new AiSuggestion(
                users.getReferenceById(userId), from, to, hash, json, source, result.model()));
        return new StoredInsight(result, source, saved.getCreatedAt(), from, to);
    }

    private Optional<StoredInsight> toStored(AiSuggestion s) {
        try {
            InsightResult result = mapper.readValue(s.getResponseJson(), InsightResult.class);
            return Optional.of(new StoredInsight(result, s.getSource(), s.getCreatedAt(),
                    s.getPeriodStart(), s.getPeriodEnd()));
        } catch (RuntimeException e) {
            log.warn("Ignoring unreadable stored suggestion {}", s.getId());
            return Optional.empty();
        }
    }
}