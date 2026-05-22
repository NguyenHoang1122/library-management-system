package com.librarymanagementsystem.service.dashboard;

import java.util.Map;

public interface DashboardService {
    Map<String, Object> getDashboardStatistics();
    Map<String, Object> getRevenueChartData(String period);
    Map<String, Object> getUserRegistrationChartData(String period);
}
