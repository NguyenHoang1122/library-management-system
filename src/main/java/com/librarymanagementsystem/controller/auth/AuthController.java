package com.librarymanagementsystem.controller.auth;

import com.librarymanagementsystem.model.user.dto.UserDTO;
import com.librarymanagementsystem.service.impl.UserServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class AuthController {
    private final UserServiceImpl userServiceImpl;

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("userDTO", new UserDTO());
        return "auth/register";
    }

    @PostMapping("/users/register")
    public String registerUser(@Valid @ModelAttribute("userDTO") UserDTO userDTO,BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        // Check validation
        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            userServiceImpl.register(userDTO);
            redirectAttributes.addFlashAttribute("message", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("userDTO", userDTO);
            return "auth/register";
        }
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "auth/login";
    }
}
