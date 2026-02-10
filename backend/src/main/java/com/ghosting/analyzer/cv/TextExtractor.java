package com.ghosting.analyzer.cv;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TextExtractor {

    private final Tika tika = new Tika();

    public String extract(MultipartFile file) {
        try {
            String text = tika.parseToString(file.getInputStream());
            if (text == null) return "";
            text = text.replace("\u0000", "").trim();
            return text;
        } catch (Exception e) {
            throw new RuntimeException("CV text extraction failed", e);
        }
    }
}
