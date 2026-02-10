
package com.ghosting.analyzer.cv.dto;

import java.time.Instant;
import java.util.UUID;

public record CvResponse(
        UUID cvId,
        String filename,
        Instant createdAt
) {}
