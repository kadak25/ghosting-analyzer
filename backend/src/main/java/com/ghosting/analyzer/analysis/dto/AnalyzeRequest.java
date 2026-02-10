package com.ghosting.analyzer.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AnalyzeRequest(
        @NotNull UUID cvId,
        @NotBlank String jobDescription,
        String company,
        String jobTitle,
        String country
) {}
