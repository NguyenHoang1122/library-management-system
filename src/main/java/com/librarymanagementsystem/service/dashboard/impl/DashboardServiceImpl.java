package com.librarymanagementsystem.service.dashboard.impl;

import com.librarymanagementsystem.repository.borrow.BorrowRequestRepository;
import com.librarymanagementsystem.repository.user.UserRepository;
import com.librarymanagementsystem.service.dashboard.DashboardService;
import com.librarymanagementsystem.service.user.AdminWalletService;
import com.librarymanagementsystem.service.borrow.BorrowService;
import com.librarymanagementsystem.repository.book.BookRepository;
import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.model.book.Book;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BorrowRequestRepository borrowRequestRepository;

    @Autowired
    private AdminWalletService adminWalletService;

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private BookRepository bookRepository;

    @Override
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalUsers = userRepository.countTotalUsers();
        
        LocalDate now = LocalDate.now();
        Map<String, Object> financeSummary = adminWalletService.getMonthlyFinanceSummary(now.getMonthValue(), now.getYear());
        
        BigDecimal totalRevenue = (BigDecimal) financeSummary.get("revenue");
        BigDecimal actualRevenue = (BigDecimal) financeSummary.get("netIncome");

        stats.put("totalUsers", totalUsers);
        stats.put("totalRevenue", totalRevenue);
        stats.put("actualRevenue", actualRevenue);
        
        return stats;
    }

    @Override
    public Map<String, Object> getRevenueChartData(String period) {
        Map<String, Object> walletChartData = adminWalletService.getFinanceChartData(period);
        List<String> labels = (List<String>) walletChartData.get("labels");
        List<Double> revenues = (List<Double>) walletChartData.get("revenues");
        List<Double> expenses = (List<Double>) walletChartData.get("expenses");
        
        List<Double> netProfits = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            double rev = revenues.get(i);
            double exp = expenses.get(i);
            netProfits.add(rev - exp); // Lợi nhuận thực tế = Thu - Chi
        }
        
        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("data", netProfits);
        
        return chartData;
    }

    @Override
    public Map<String, Object> getUserRegistrationChartData(String period) {
        List<Object[]> usersData;
        if ("year".equalsIgnoreCase(period)) {
            usersData = userRepository.countYearlyNewUsers();
        } else if ("month".equalsIgnoreCase(period)) {
            usersData = userRepository.countMonthlyNewUsers();
        } else {
            usersData = userRepository.countDailyNewUsers();
        }
        
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        
        for (int i = usersData.size() - 1; i >= 0; i--) {
            Object[] row = usersData.get(i);
            labels.add(row[0] != null ? row[0].toString() : "N/A");
            data.add(row[1] != null ? ((Number) row[1]).longValue() : 0L);
        }
        
        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("data", data);
        
        return chartData;
    }

    @Override
    public Map<String, Object> getLibrarianDashboardData() {
        Map<String, Object> data = new HashMap<>();

        // 1. Số liệu chỉ số vận hành thời gian thực (Operational KPIs)
        long pendingBorrows = borrowService.getPendingRequests(null, Pageable.unpaged()).getTotalElements();
        long pendingReturns = borrowService.getPendingReturnRequests(null, Pageable.unpaged()).getTotalElements();
        long activeDeliveries = borrowService.getApprovedRequests(null, Pageable.unpaged()).getTotalElements();
        
        List<BorrowTransaction> activeBorrows = borrowService.getAllActiveBorrows();
        long overdueCount = activeBorrows.stream()
                .filter(tx -> borrowService.isOverdue(tx.getId()))
                .count();

        // 2. Thống kê kho sách vật lý
        List<Book> allBooks = bookRepository.findAll();
        long totalTitles = allBooks.size();
        long totalPhysicalBooks = allBooks.stream().mapToLong(Book::getQuantity).sum();
        
        // Cảnh báo hết sách hoặc tồn kho dưới 10 cuốn (Stock Warning < 10)
        List<Book> lowStockBooks = allBooks.stream()
                .filter(b -> b.getQuantity() != null && b.getQuantity() < 10)
                .sorted((b1, b2) -> Integer.compare(b1.getQuantity(), b2.getQuantity()))
                .limit(10) // Lấy tối đa 10 đầu truyện sắp cạn kệ nhất
                .collect(Collectors.toList());

        // 3. Số cuốn sách đang nằm ngoài kho (đang cho mượn)
        long booksLentOut = activeBorrows.stream()
                .mapToLong(tx -> tx.getItems().stream().filter(item -> item.getReturnDate() == null).count())
                .sum();
        long booksInWarehouse = totalPhysicalBooks - booksLentOut;

        // 4. Thống kê Top 5 đầu truyện được mượn nhiều nhất trong kho hiện hành
        Map<Book, Long> bookBorrowCounts = activeBorrows.stream()
                .flatMap(tx -> tx.getItems().stream())
                .map(item -> item.getBookCopy() != null ? item.getBookCopy().getBook() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(b -> b, Collectors.counting()));

        List<Map.Entry<Book, Long>> topBorrowed = bookBorrowCounts.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(5)
                .collect(Collectors.toList());

        // 5. Nạp dữ liệu vào bản đồ kết quả
        data.put("pendingBorrows", pendingBorrows);
        data.put("pendingReturns", pendingReturns);
        data.put("activeDeliveries", activeDeliveries);
        data.put("overdueCount", overdueCount);
        data.put("totalTitles", totalTitles);
        data.put("totalPhysicalBooks", totalPhysicalBooks);
        data.put("lowStockBooks", lowStockBooks);
        data.put("booksLentOut", booksLentOut);
        data.put("booksInWarehouse", booksInWarehouse >= 0 ? booksInWarehouse : 0);
        data.put("topBorrowed", topBorrowed);

        return data;
    }
}
