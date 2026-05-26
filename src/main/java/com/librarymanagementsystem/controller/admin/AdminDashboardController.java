package com.librarymanagementsystem.controller.admin;

import com.librarymanagementsystem.service.dashboard.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping
    public String viewDashboard(Model model) {
        Map<String, Object> stats = dashboardService.getDashboardStatistics();
        model.addAllAttributes(stats);
        return "admin/dashboard";
    }

    @GetMapping("/chart/revenue")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRevenueChart(@RequestParam(defaultValue = "day") String period) {
        return ResponseEntity.ok(dashboardService.getRevenueChartData(period));
    }

    @GetMapping("/chart/users")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUsersChart(@RequestParam(defaultValue = "day") String period) {
        return ResponseEntity.ok(dashboardService.getUserRegistrationChartData(period));
    }
}
