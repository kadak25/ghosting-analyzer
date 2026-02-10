package com.ghosting.analyzer.analysis;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class GhostingAnalyzerEngine {

    // MVP: Basit keyword match + readability + dummy reasons
    public String analyze(String cvText, String jdText) {
        var cv = normalize(cvText);
        var jd = normalize(jdText);

        var jdKeywords = extractKeywords(jd);
        int matched = 0;
        List<String> missing = new ArrayList<>();

        for (String k : jdKeywords) {
            if (cv.contains(k)) matched++;
            else missing.add(k);
        }

        int total = Math.max(1, jdKeywords.size());
        int matchScore = (int) Math.round((matched * 100.0) / total);

        int atsReadability = estimateReadability(cvText);
        double ghostProb = clamp01(0.35 + (1 - matchScore / 100.0) * 0.45 + (1 - atsReadability / 100.0) * 0.20);

        // Limit missing list for UI
        List<String> missingTop = missing.stream().limit(10).toList();


        return """
      {
        "ghosting_probability": %s,
        "match_score": %d,
        "ats_readability_score": %d,
        "seniority_guess": "%s",
        "role_guess": "%s",
        "top_rejection_reasons": [
          {"reason": "İlan anahtar kelimeleri CV'de eksik", "confidence": 0.74},
          {"reason": "Deneyim maddeleri ölçülebilir sonuç içermiyor olabilir", "confidence": 0.62}
        ],
        "missing_skills": %s,
        "fixes": [
          {"area": "Özet", "action": "İlanla aynı role odaklı 1 satırlık net özet ekle"},
          {"area": "Skills", "action": "Eksik teknolojileri varsa skills'e ekle; yoksa 'Learning' bölümüne koy"},
          {"area": "Deneyim", "action": "Her maddeyi etki + metrik ile yaz (örn: %%20 hızlandı)"}
        ],
        "rewrite_suggestions": [
          {"original": "Developed APIs", "improved": "Built REST APIs and improved response times via caching and indexing"}
        ]
      }
    """.formatted(
                String.format(Locale.US, "%.2f", ghostProb),
                matchScore,
                atsReadability,
                guessSeniority(matchScore),
                guessRole(jdText),
                toJsonArray(missingTop)
        );
    }

    private String normalize(String s) {
        return (s == null ? "" : s).toLowerCase(Locale.ROOT);
    }

    private List<String> extractKeywords(String jd) {

        var tokens = jd.split("[^a-z0-9+#.]+");
        var stop = Set.of("and","or","the","with","for","to","in","of","a","an","on","as","is","are","we","you","our","your","will","be","at");
        var techLike = Pattern.compile("^[a-z][a-z0-9+#.]{1,20}$");

        Map<String,Integer> freq = new HashMap<>();
        for (String t : tokens) {
            if (t.length() < 2) continue;
            if (stop.contains(t)) continue;
            if (!techLike.matcher(t).matches()) continue;
            freq.put(t, freq.getOrDefault(t, 0) + 1);
        }

        return freq.entrySet().stream()
                .sorted((a,b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .limit(25)
                .toList();
    }

    private int estimateReadability(String cvText) {

        if (cvText == null || cvText.isBlank()) return 20;
        int len = cvText.length();
        long weird = cvText.chars().filter(ch -> ch == '\u0000').count();
        int score = 80;

        if (len > 12000) score -= 15;
        if (len > 20000) score -= 15;
        if (weird > 0) score -= 20;

        return Math.max(10, Math.min(100, score));
    }

    private double clamp01(double x) {
        if (x < 0) return 0;
        if (x > 1) return 1;
        return x;
    }

    private String guessSeniority(int matchScore) {
        if (matchScore >= 75) return "MID";
        if (matchScore >= 55) return "JR";
        return "JR";
    }

    private String guessRole(String jdText) {
        var t = (jdText == null ? "" : jdText).toLowerCase(Locale.ROOT);
        if (t.contains("backend")) return "Backend Developer";
        if (t.contains("frontend")) return "Frontend Developer";
        if (t.contains("qa") || t.contains("test")) return "QA / Test Automation";
        return "Software Developer";
    }

    private String toJsonArray(List<String> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < items.size(); i++) {
            sb.append("\"").append(items.get(i).replace("\"","")).append("\"");
            if (i < items.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
