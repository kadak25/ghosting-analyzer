// DOSYA YOLU: backend/src/main/java/com/ghosting/analyzer/cv/CvController.java
package com.ghosting.analyzer.cv;

import com.ghosting.analyzer.cv.dto.CvResponse;
import com.ghosting.analyzer.security.JwtService;
import com.ghosting.analyzer.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cvs")
@RequiredArgsConstructor
public class CvController {

    private final CvRepository cvRepository;
    private final UserRepository userRepository;
    private final TextExtractor textExtractor;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CvResponse upload(@RequestPart("file") MultipartFile file, Authentication auth) {
        var jwtUser = (JwtService.JwtUser) auth.getPrincipal();
        UUID userId = UUID.fromString(jwtUser.userId());

        var user = userRepository.findById(userId).orElseThrow();

        String text = textExtractor.extract(file);
        if (text.length() < 50) {
            throw new IllegalArgumentException("Extracted text too short. Try another CV file.");
        }

        var cv = Cv.builder()
                .user(user)
                .filename(file.getOriginalFilename() == null ? "cv" : file.getOriginalFilename())
                .rawText(text)
                .build();

        cv = cvRepository.save(cv);

        return new CvResponse(cv.getId(), cv.getFilename(), cv.getCreatedAt());
    }

    @GetMapping
    public List<CvResponse> list(Authentication auth) {
        var jwtUser = (JwtService.JwtUser) auth.getPrincipal();
        UUID userId = UUID.fromString(jwtUser.userId());

        return cvRepository.findAllByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(c -> new CvResponse(c.getId(), c.getFilename(), c.getCreatedAt()))
                .toList();
    }
}
