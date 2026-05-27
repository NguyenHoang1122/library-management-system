package com.librarymanagementsystem.service.user;

import com.librarymanagementsystem.model.user.AdminWalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Map;

public interface AdminWalletService {
    
    /**
     * Ghi nhận một giao dịch tài chính cho ví ảo Admin.
     * Đồng thời tự động cập nhật số dư tài khoản của Admin trong Cơ sở dữ liệu.
     */
    AdminWalletTransaction logTransaction(BigDecimal amount, String type, String description, Long referenceId);

    /**
     * Lấy số dư hiện tại của Ví ảo Admin.
     */
    BigDecimal getAdminBalance();

    /**
     * Lấy lịch sử danh sách giao dịch ví ảo có phân trang.
     */
    Page<AdminWalletTransaction> getTransactionHistory(Pageable pageable);

    /**
     * Lấy báo cáo tổng hợp Thu, Chi và Thực thu (Lợi nhuận) của một tháng cụ thể.
     */
    Map<String, Object> getMonthlyFinanceSummary(int month, int year);

    /**
     * Lấy dữ liệu vẽ biểu đồ tài chính so sánh Thu và Chi (theo ngày hoặc theo tháng).
     */
    Map<String, Object> getFinanceChartData(String period);
}
