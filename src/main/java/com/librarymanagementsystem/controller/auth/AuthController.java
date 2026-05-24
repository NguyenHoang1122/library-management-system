package com.librarymanagementsystem.controller.auth;

import com.librarymanagementsystem.model.user.dto.UserDTO;
import com.librarymanagementsystem.service.user.impl.UserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
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
    public String showLoginPage(@RequestParam(value = "error", required = false) String error,
                                HttpServletRequest request,
                                Model model) {
        if (error != null) {
            model.addAttribute("error", true);
            HttpSession session = request.getSession(false);
            if (session != null) {
                Object lastException = session.getAttribute("SPRING_SECURITY_LAST_EXCEPTION");
                if (lastException instanceof DisabledException) {
                    model.addAttribute("bannedError", true);
                    model.addAttribute("errorMsg", "Tài khoản đã bị khóa.");
                }
            }
        }
        return "auth/login";
    }
}
