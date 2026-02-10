package com.ghosting.analyzer.insight;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "insight_events")
public class InsightEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String country;

    private String roleGuess;
    private String seniorityGuess;

    @Column(nullable = false)
    private Integer matchScore;

    @Column(nullable = false)
    private Integer atsReadabilityScore;

    @Column(columnDefinition = "text[]")
    private String[] missingSkills;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
