package com.librarymanagementsystem.service.dashboard;

import java.util.Map;

public interface DashboardService {
    Map<String, Object> getDashboardStatistics();
    Map<String, Object> getRevenueChartData();
    Map<String, Object> getUserRegistrationChartData();
}
