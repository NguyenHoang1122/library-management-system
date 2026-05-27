package com.librarymanagementsystem.controller.admin;

import com.librarymanagementsystem.model.user.AdminWalletTransaction;
import com.librarymanagementsystem.service.dashboard.DashboardService;
import com.librarymanagementsystem.service.user.AdminWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private AdminWalletService adminWalletService;

    // Hiển thị giao diện chính của Dashboard, tích hợp 2 tab Thống kê và Tài chính
    @GetMapping
    public String viewDashboard(Model model,
                                @RequestParam(defaultValue = "stats") String tab,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size) {
        // 1. Lấy dữ liệu Thống kê Thư viện (Tab 1)
        Map<String, Object> stats = dashboardService.getDashboardStatistics();
        model.addAllAttributes(stats);

        // 2. Lấy dữ liệu Báo cáo Tài chính & Ví ảo (Tab 2)
        LocalDate now = LocalDate.now();
        Map<String, Object> financeSummary = adminWalletService.getMonthlyFinanceSummary(now.getMonthValue(), now.getYear());
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminWalletTransaction> transactions = adminWalletService.getTransactionHistory(pageable);
        
        model.addAttribute("balance", adminWalletService.getAdminBalance());
        model.addAttribute("revenue", financeSummary.get("revenue"));
        model.addAttribute("expense", financeSummary.get("expense"));
        model.addAttribute("netIncome", financeSummary.get("netIncome"));
        model.addAttribute("transactions", transactions.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactions.getTotalPages());
        
        // Lưu trạng thái tab hiện tại để kích hoạt giao diện
        model.addAttribute("activeTab", tab);

        return "admin/dashboard";
    }

    // API vẽ biểu đồ Doanh thu thực tế / Lợi nhuận (đã được sửa đổi hiển thị lợi nhuận thực tế)
    @GetMapping("/chart/revenue")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRevenueChart(@RequestParam(defaultValue = "day") String period) {
        return ResponseEntity.ok(dashboardService.getRevenueChartData(period));
    }

    // API vẽ biểu đồ User đăng ký mới
    @GetMapping("/chart/users")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUsersChart(@RequestParam(defaultValue = "day") String period) {
        return ResponseEntity.ok(dashboardService.getUserRegistrationChartData(period));
    }

    // API vẽ biểu đồ so sánh Thu - Chi của trang Tài chính
    @GetMapping("/chart/finance")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFinanceChart(@RequestParam(defaultValue = "day") String period) {
        return ResponseEntity.ok(adminWalletService.getFinanceChartData(period));
    }
}
