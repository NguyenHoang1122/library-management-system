package com.librarymanagementsystem.service.book;

import java.util.List;

public interface GeminiService {
    ModerationResult moderateText(String text, String apiKey);
    
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    class ModerationResult {
        private boolean sensitive;
        private List<String> sensitiveWordsFound;
        private String censoredText;
        private String reason;
    }
}
