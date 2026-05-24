package com.librarymanagementsystem.controller.admin;

import com.librarymanagementsystem.model.book.SensitiveWord;
import com.librarymanagementsystem.repository.book.SensitiveWordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/sensitive-words")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSensitiveWordController {

    private final SensitiveWordRepository sensitiveWordRepository;

    @GetMapping
    public String listWords(Model model) {
        model.addAttribute("words", sensitiveWordRepository.findAll());
        return "admin/sensitive-words";
    }

    @PostMapping("/add")
    public String addWord(@RequestParam("word") String word, RedirectAttributes redirectAttributes) {
        if (word == null || word.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Từ không được để trống!");
            return "redirect:/admin/sensitive-words";
        }
        String cleanWord = word.trim().toLowerCase();
        if (sensitiveWordRepository.existsByWordIgnoreCase(cleanWord)) {
            redirectAttributes.addFlashAttribute("error", "Từ này đã tồn tại trong danh sách!");
            return "redirect:/admin/sensitive-words";
        }
        
        sensitiveWordRepository.save(new SensitiveWord(null, cleanWord));
        redirectAttributes.addFlashAttribute("message", "Thêm từ nhạy cảm thành công!");
        return "redirect:/admin/sensitive-words";
    }

    @GetMapping("/delete/{id}")
    public String deleteWord(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        sensitiveWordRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Đã xóa từ nhạy cảm!");
        return "redirect:/admin/sensitive-words";
    }
}
