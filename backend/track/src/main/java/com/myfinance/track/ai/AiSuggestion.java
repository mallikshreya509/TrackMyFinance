package com.myfinance.track.ai;

import com.myfinance.track.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "ai_suggestions")
public class AiSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "data_hash", nullable = false, length = 64)
    private String dataHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_json", nullable = false)
    private String responseJson;

    // VARCHAR on purpose: stops Hibernate expecting a native MySQL ENUM column.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private AiSource source;

    @Column(length = 50)
    private String model;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AiSuggestion() {}

    public AiSuggestion(User user, LocalDate periodStart, LocalDate periodEnd,
                        String dataHash, String responseJson, AiSource source, String model) {
        this.user = user;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.dataHash = dataHash;
        this.responseJson = responseJson;
        this.source = source;
        this.model = model;
    }

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public String getDataHash() { return dataHash; }
    public String getResponseJson() { return responseJson; }
    public AiSource getSource() { return source; }
    public String getModel() { return model; }
    public Instant getCreatedAt() { return createdAt; }
}