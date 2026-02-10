package com.ghosting.analyzer.analysis;

import com.ghosting.analyzer.analysis.dto.AnalyzeRequest;
import com.ghosting.analyzer.analysis.dto.AnalyzeResponse;
import com.ghosting.analyzer.cv.CvRepository;
import com.ghosting.analyzer.insight.InsightEvent;
import com.ghosting.analyzer.insight.InsightEventRepository;
import com.ghosting.analyzer.security.JwtService;
import com.ghosting.analyzer.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisRepository analysisRepository;
    private final CvRepository cvRepository;
    private final UserRepository userRepository;
    private final InsightEventRepository insightEventRepository;

    private final GhostingAnalyzerEngine engine;          // deterministic scores
    private final HuggingFaceAiService hfAiService;        // AI commentary
    private final AiPromptBuilder aiPromptBuilder;         // prompt

    @PostMapping
    public AnalyzeResponse analyze(@Valid @RequestBody AnalyzeRequest req, Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        var jwtUser = (JwtService.JwtUser) auth.getPrincipal();
        UUID userId = UUID.fromString(jwtUser.userId());

        var user = userRepository.findById(userId).orElseThrow();
        var cv = cvRepository.findById(req.cvId()).orElseThrow();

        if (!cv.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CV does not belong to user");
        }

        String country = (req.country() == null || req.country().isBlank()) ? "TR" : req.country().trim();

        // 1) engine ile skorları garanti al (match_score + ats)
        String engineJson = engine.analyze(cv.getRawText(), req.jobDescription());

        int engineMatch = extractInt(engineJson, "\"match_score\":");
        int engineAts = extractInt(engineJson, "\"ats_readability_score\":");
        double engineGhost = extractDouble(engineJson, "\"ghosting_probability\":");

        // 2) AI ile yorum üret (eksik skill, reasons, fixes vs)
        String aiJson = runAiOrNull(cv.getRawText(), req.jobDescription(), country, req.company(), req.jobTitle());

        // 3) AI çalıştıysa: skor alanlarını engine ile override et
        String finalJson;
        if (aiJson != null) {
            finalJson = mergeScoresIntoAiJson(aiJson, engineGhost, engineMatch, engineAts);
            System.out.println("[AI] ✅ Using AI result (scores overridden by engine)");
        } else {
            finalJson = engineJson;
            System.out.println("[AI] ❌ Using engine only");
        }

        var analysis = Analysis.builder()
                .user(user)
                .cv(cv)
                .country(country)
                .company(req.company())
                .jobTitle(req.jobTitle())
                .jobDescription(req.jobDescription())
                .resultJson(finalJson)
                .build();

        analysis = analysisRepository.save(analysis);

        var event = InsightEvent.builder()
                .country(country)
                .matchScore(extractInt(finalJson, "\"match_score\":"))
                .atsReadabilityScore(extractInt(finalJson, "\"ats_readability_score\":"))
                .roleGuess(extractString(finalJson, "\"role_guess\":"))
                .seniorityGuess(extractString(finalJson, "\"seniority_guess\":"))
                .missingSkills(null)
                .build();

        insightEventRepository.save(event);

        return new AnalyzeResponse(
                analysis.getId(),
                cv.getId(),
                analysis.getResultJson(),
                analysis.getCreatedAt()
        );
    }

    @GetMapping
    public List<AnalyzeResponse> history(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        var jwtUser = (JwtService.JwtUser) auth.getPrincipal();
        UUID userId = UUID.fromString(jwtUser.userId());

        return analysisRepository.findAllByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(a -> new AnalyzeResponse(a.getId(), a.getCv().getId(), a.getResultJson(), a.getCreatedAt()))
                .toList();
    }

    private String runAiOrNull(String cvText, String jdText, String country, String company, String jobTitle) {
        try {
            String prompt = aiPromptBuilder.build(cvText, jdText, country, company, jobTitle);
            String aiText = hfAiService.generate(prompt);
            if (aiText == null) return null;

            String trimmed = aiText.trim();
            System.out.println("[AI] Raw(first 250): " + trimmed.substring(0, Math.min(250, trimmed.length())));

            String jsonOnly = extractJsonObject(trimmed);
            if (jsonOnly == null) return null;

            // minimum alan kontrolü
            if (!jsonOnly.contains("\"ghosting_probability\"") || !jsonOnly.contains("\"match_score\"")) return null;

            return jsonOnly;
        } catch (Exception e) {
            System.out.println("[AI] Exception -> AI disabled for this run. " + e.getMessage());
            return null;
        }
    }

    private String extractJsonObject(String text) {
        if (text == null) return null;
        int a = text.indexOf('{');
        int b = text.lastIndexOf('}');
        if (a < 0 || b < 0 || b <= a) return null;
        return text.substring(a, b + 1).trim();
    }

    // AI json içindeki score alanlarını engine değerleriyle değiştir (basit string replace)
    private String mergeScoresIntoAiJson(String aiJson, double ghostProb, int matchScore, int atsScore) {
        String out = aiJson;

        // ghosting_probability: sayı formatı (0.80 gibi)
        out = out.replaceAll("\"ghosting_probability\"\\s*:\\s*([0-9]+\\.?[0-9]*)", "\"ghosting_probability\": " + String.format(java.util.Locale.US, "%.2f", ghostProb));

        // match_score / ats_readability_score: integer
        out = out.replaceAll("\"match_score\"\\s*:\\s*([0-9]+)", "\"match_score\": " + matchScore);
        out = out.replaceAll("\"ats_readability_score\"\\s*:\\s*([0-9]+)", "\"ats_readability_score\": " + atsScore);

        return out;
    }

    private int extractInt(String json, String key) {
        try {
            int i = json.indexOf(key);
            if (i < 0) return 0;
            int start = i + key.length();
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == ' ')) end++;
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private double extractDouble(String json, String key) {
        try {
            int i = json.indexOf(key);
            if (i < 0) return 0.0;
            int start = i + key.length();
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == ' ')) end++;
            return Double.parseDouble(json.substring(start, end).trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String extractString(String json, String key) {
        try {
            int i = json.indexOf(key);
            if (i < 0) return null;
            int start = json.indexOf("\"", i + key.length());
            int end = json.indexOf("\"", start + 1);
            return json.substring(start + 1, end);
        } catch (Exception e) {
            return null;
        }
    }
}
