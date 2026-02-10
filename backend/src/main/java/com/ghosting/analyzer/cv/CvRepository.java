package com.ghosting.analyzer.cv;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CvRepository extends JpaRepository<Cv, UUID> {
    List<Cv> findAllByUser_IdOrderByCreatedAtDesc(UUID userId);
}
