package com.ghosting.analyzer.analysis;

import org.springframework.stereotype.Component;

@Component
public class AiPromptBuilder {

    public String build(String cvText, String jdText, String country, String company, String jobTitle) {
        String c = (country == null || country.isBlank()) ? "TR" : country.trim();
        String comp = (company == null || company.isBlank()) ? "Unknown" : company.trim();
        String jt = (jobTitle == null || jobTitle.isBlank()) ? "Unknown" : jobTitle.trim();

        return """
You are a brutally honest senior technical recruiter + ATS expert.

TASK:
Analyze the CV vs Job Description and return ONLY valid JSON (no markdown, no extra text).

LANGUAGE RULES:
- Use Turkish for reasons/actions
- missing_skills must be technical terms in English (e.g., "spring security", "docker", "kafka")
- Be realistic and strict (do not be overly positive)

OUTPUT JSON SCHEMA (MUST match keys exactly):
{
  "ghosting_probability": 0.00,
  "match_score": 0,
  "ats_readability_score": 0,
  "seniority_guess": "JR",
  "role_guess": "string",
  "top_rejection_reasons": [{"reason":"string","confidence":0.0}],
  "missing_skills": ["string"],
  "fixes": [{"area":"string","action":"string"}],
  "rewrite_suggestions": [{"original":"string","improved":"string"}]
}

CONTEXT:
Country: %s
Company: %s
JobTitle: %s

CV TEXT:
<<<
%s
>>>

JOB DESCRIPTION:
<<<
%s
>>>

Return ONLY JSON now.
""".formatted(c, comp, jt, safe(cvText), safe(jdText));
    }

    private String safe(String s) {
        if (s == null) return "";

        if (s.length() > 12000) return s.substring(0, 12000);
        return s;
    }
}
