package com.ghosting.analyzer.analysis.dto;

import java.time.Instant;
import java.util.UUID;

public record AnalyzeResponse(
        UUID analysisId,
        UUID cvId,
        String resultJson,
        Instant createdAt
) {}
