package com.librarymanagementsystem.controller.admin;

import com.librarymanagementsystem.model.book.SensitiveWord;
import com.librarymanagementsystem.service.book.GeminiService;
import com.librarymanagementsystem.service.book.SensitiveWordService;
import com.librarymanagementsystem.service.book.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin/sensitive-words")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSensitiveWordController {

    private final SensitiveWordService sensitiveWordService;
    private final SystemSettingService systemSettingService;
    private final GeminiService geminiService;

    @GetMapping
    public String listWords(Model model) {
        model.addAttribute("words", sensitiveWordService.getAllSensitiveWords());
        model.addAttribute("activePage", "sensitivewords");
        
        // Add AI settings
        model.addAttribute("aiEnabled", systemSettingService.isAiModerationEnabled());
        model.addAttribute("aiApiKey", systemSettingService.getAiApiKey());
        model.addAttribute("aiAction", systemSettingService.getAiModerationAction());
        
        return "admin/sensitive-words";
    }

    @PostMapping("/add")
    public String addWord(@RequestParam("word") String word, RedirectAttributes redirectAttributes) {
        try {
            sensitiveWordService.addSensitiveWord(word);
            redirectAttributes.addFlashAttribute("message", "Thêm từ nhạy cảm thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/sensitive-words";
    }

    @GetMapping("/delete/{id}")
    public String deleteWord(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        sensitiveWordService.deleteSensitiveWord(id);
        redirectAttributes.addFlashAttribute("message", "Đã xóa từ nhạy cảm!");
        return "redirect:/admin/sensitive-words";
    }

    @PostMapping("/ai-settings")
    public String updateAiSettings(@RequestParam(value = "aiEnabled", required = false) String aiEnabled,
                                   @RequestParam("aiApiKey") String aiApiKey,
                                   @RequestParam("aiAction") String aiAction,
                                   RedirectAttributes redirectAttributes) {
        try {
            systemSettingService.saveSetting("ai_moderation_enabled", aiEnabled != null ? "true" : "false");
            systemSettingService.saveSetting("ai_api_key", aiApiKey.trim());
            systemSettingService.saveSetting("ai_moderation_action", aiAction);
            redirectAttributes.addFlashAttribute("message", "Cấu hình AI đã được lưu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi lưu cấu hình: " + e.getMessage());
        }
        return "redirect:/admin/sensitive-words";
    }

    @PostMapping("/ai-test")
    @ResponseBody
    public ResponseEntity<?> testAiModeration(@RequestParam("text") String text) {
        try {
            String apiKey = systemSettingService.getAiApiKey();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of("success", false, "error", "Vui lòng nhập và lưu API Key trước khi thử nghiệm!"));
            }
            GeminiService.ModerationResult result = geminiService.moderateText(text, apiKey);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "sensitive", result.isSensitive(),
                "sensitiveWordsFound", result.getSensitiveWordsFound(),
                "censoredText", result.getCensoredText(),
                "reason", result.getReason()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
