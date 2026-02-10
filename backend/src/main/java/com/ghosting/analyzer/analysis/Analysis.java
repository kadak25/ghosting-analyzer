package com.ghosting.analyzer.analysis;

import com.ghosting.analyzer.cv.Cv;
import com.ghosting.analyzer.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "analyses")
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    private Cv cv;

    private String jobTitle;
    private String company;

    @Column(nullable = false)
    private String country; // TR default


    @Column(name = "job_description", nullable = false, columnDefinition = "text")
    private String jobDescription;

    @Column(name = "result_json", columnDefinition = "jsonb", nullable = false)
    private String resultJson;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (country == null || country.isBlank()) country = "TR";
    }
}
