package com.librarymanagementsystem.service.user.impl;

import com.librarymanagementsystem.model.user.AdminWalletTransaction;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.model.user.status.RoleStatus;
import com.librarymanagementsystem.repository.user.AdminWalletTransactionRepository;
import com.librarymanagementsystem.repository.user.UserRepository;
import com.librarymanagementsystem.service.user.AdminWalletService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminWalletServiceImpl implements AdminWalletService {

    private final UserRepository userRepository;
    private final AdminWalletTransactionRepository transactionRepository;

    @Override
    public synchronized AdminWalletTransaction logTransaction(BigDecimal amount, String type, String description, Long referenceId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        List<User> admins = userRepository.findByRoleRoleName(RoleStatus.ROLE_ADMIN);
        if (admins.isEmpty()) {
            throw new RuntimeException("Không tìm thấy tài khoản quản trị viên (Admin) để cập nhật ví.");
        }
        User admin = admins.get(0);

        BigDecimal oldBalance = admin.getBalance() != null ? admin.getBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = oldBalance.add(amount);
        admin.setBalance(newBalance);
        userRepository.save(admin);

        AdminWalletTransaction tx = AdminWalletTransaction.builder()
                .amount(amount)
                .balanceAfter(newBalance)
                .type(type)
                .description(description)
                .transactionDate(LocalDateTime.now())
                .referenceId(referenceId)
                .build();

        return transactionRepository.save(tx);
    }

    @Override
    public BigDecimal getAdminBalance() {
        List<User> admins = userRepository.findByRoleRoleName(RoleStatus.ROLE_ADMIN);
        if (admins.isEmpty()) {
            return BigDecimal.ZERO;
        }
        User admin = admins.get(0);
        return admin.getBalance() != null ? admin.getBalance() : BigDecimal.ZERO;
    }

    @Override
    public Page<AdminWalletTransaction> getTransactionHistory(Pageable pageable) {
        return transactionRepository.findAllByOrderByTransactionDateDesc(pageable);
    }

    @Override
    public Map<String, Object> getMonthlyFinanceSummary(int month, int year) {
        BigDecimal revenue = transactionRepository.sumTotalRevenueByMonth(month, year);
        BigDecimal expense = transactionRepository.sumTotalExpenseByMonth(month, year);

        if (revenue == null) revenue = BigDecimal.ZERO;
        if (expense == null) expense = BigDecimal.ZERO;

        BigDecimal netIncome = revenue.subtract(expense);

        Map<String, Object> summary = new HashMap<>();
        summary.put("revenue", revenue);
        summary.put("expense", expense);
        summary.put("netIncome", netIncome);
        return summary;
    }

    @Override
    public Map<String, Object> getFinanceChartData(String period) {
        List<Object[]> queryData;
        if ("month".equalsIgnoreCase(period)) {
            queryData = transactionRepository.getMonthlyFinanceChartData();
        } else {
            queryData = transactionRepository.getDailyFinanceChartData();
        }

        List<String> labels = new ArrayList<>();
        List<Double> revenues = new ArrayList<>();
        List<Double> expenses = new ArrayList<>();

        // Duyệt ngược danh sách để giữ thứ tự thời gian tăng dần (từ cũ nhất đến mới nhất)
        for (int i = queryData.size() - 1; i >= 0; i--) {
            Object[] row = queryData.get(i);
            labels.add(row[0] != null ? row[0].toString() : "N/A");
            revenues.add(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);
            expenses.add(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0);
        }

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("revenues", revenues);
        chartData.put("expenses", expenses);
        return chartData;
    }
}
