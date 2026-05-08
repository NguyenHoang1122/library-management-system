package com.librarymanagementsystem.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            for(GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
                String role = grantedAuthority.getAuthority();
                if(role.equals("ROLE_ADMIN")) {
                    return "redirect:/admin/dashboard";
                } else if(role.equals("ROLE_LIBRARIAN")) {
                    return "redirect:/librarian/dashboard";
                } else if(role.equals("ROLE_USER")) {
                    return "redirect:/user/dashboard";
                }
            }
        }
        return "redirect:/";
    }
}
