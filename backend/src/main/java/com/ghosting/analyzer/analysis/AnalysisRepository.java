package com.ghosting.analyzer.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {
    List<Analysis> findAllByUser_IdOrderByCreatedAtDesc(UUID userId);
}
