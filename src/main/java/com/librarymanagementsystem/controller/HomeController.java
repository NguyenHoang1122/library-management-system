package com.librarymanagementsystem.controller;

import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.service.BookService;
import com.librarymanagementsystem.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private BookService bookService;

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userService.findByUserName(authentication.getName()).orElse(null);
            if (user != null) {
                model.addAttribute("user", user);
            }
        }

        model.addAttribute("newestBooks", bookService.getNewestBooks());
        model.addAttribute("hotBooks", bookService.getHotBooks());
        model.addAttribute("tuTienBooks", bookService.getBooksByCategoryName("Tu tiên"));
        model.addAttribute("truyenTeenBooks", bookService.getBooksByCategoryName("Truyện teen"));

        return "home";
    }
}
