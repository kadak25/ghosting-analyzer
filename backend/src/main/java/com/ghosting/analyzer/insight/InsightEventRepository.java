package com.ghosting.analyzer.insight;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InsightEventRepository extends JpaRepository<InsightEvent, UUID> {}
