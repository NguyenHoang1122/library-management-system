package com.librarymanagementsystem.service.dashboard.impl;

import com.librarymanagementsystem.repository.borrow.BorrowRequestRepository;
import com.librarymanagementsystem.repository.user.UserRepository;
import com.librarymanagementsystem.service.dashboard.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BorrowRequestRepository borrowRequestRepository;

    @Override
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalUsers = userRepository.countTotalUsers();
        Double totalRevenue = borrowRequestRepository.sumTotalRevenueCurrentMonth();
        Double totalCost = borrowRequestRepository.sumTotalCostCurrentMonth();
        
        if (totalRevenue == null) totalRevenue = 0.0;
        if (totalCost == null) totalCost = 0.0;
        
        Double actualRevenue = totalRevenue - totalCost;

        stats.put("totalUsers", totalUsers);
        stats.put("totalRevenue", totalRevenue);
        stats.put("actualRevenue", actualRevenue);
        
        return stats;
    }

    @Override
    public Map<String, Object> getRevenueChartData(String period) {
        List<Object[]> revenueData;
        if ("year".equalsIgnoreCase(period)) {
            revenueData = borrowRequestRepository.getYearlyRevenue();
        } else if ("month".equalsIgnoreCase(period)) {
            revenueData = borrowRequestRepository.getMonthlyRevenue();
        } else {
            revenueData = borrowRequestRepository.getDailyRevenue();
        }
        
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        
        for (int i = revenueData.size() - 1; i >= 0; i--) { // Reverse to show oldest to newest
            Object[] row = revenueData.get(i);
            labels.add(row[0] != null ? row[0].toString() : "N/A");
            data.add(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);
        }
        
        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("data", data);
        
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
}
