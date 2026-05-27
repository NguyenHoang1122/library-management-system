package com.librarymanagementsystem.controller.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/finance")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFinanceController {

    // Chuyển hướng trực tiếp về Tab Tài chính trong Dashboard chính để đồng bộ giao diện thống nhất
    @GetMapping
    public String redirectFinance(@RequestParam(defaultValue = "0") int page) {
        return "redirect:/admin/dashboard?tab=finance&page=" + page;
    }
}
