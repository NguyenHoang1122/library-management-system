document.addEventListener("DOMContentLoaded", function() {
    // Định dạng màu sắc và đường lưới biểu đồ mặc định cho giao diện tối
    Chart.defaults.color = '#888';
    Chart.defaults.borderColor = '#2d2d2d';

    let revenueChartInstance = null;
    let usersChartInstance = null;
    let financeChartInstance = null;

    // 1. Khởi tạo Biểu đồ Lợi nhuận thực tế (Doanh thu thực tế) cho Tab 1
    function loadRevenueChart(period) {
        fetch('/admin/dashboard/chart/revenue?period=' + period)
            .then(response => response.json())
            .then(data => {
                const ctx = document.getElementById('revenueChart').getContext('2d');
                if (revenueChartInstance) {
                    revenueChartInstance.destroy();
                }
                revenueChartInstance = new Chart(ctx, {
                    type: 'bar',
                    data: {
                        labels: data.labels,
                        datasets: [{
                            label: 'Lợi nhuận thực tế (VNĐ)',
                            data: data.data,
                            backgroundColor: 'rgba(46, 204, 113, 0.6)',
                            borderColor: 'rgba(46, 204, 113, 1)',
                            borderWidth: 1.5,
                            borderRadius: 4
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        scales: {
                            y: {
                                beginAtZero: true,
                                ticks: {
                                    callback: function(value) {
                                        return value.toLocaleString('vi-VN') + ' đ';
                                    }
                                }
                            }
                        },
                        plugins: {
                            tooltip: {
                                callbacks: {
                                    label: function(context) {
                                        let label = context.dataset.label || '';
                                        if (label) {
                                            label += ': ';
                                        }
                                        if (context.parsed.y !== null) {
                                            label += context.parsed.y.toLocaleString('vi-VN') + ' đ';
                                        }
                                        return label;
                                    }
                                }
                            }
                        }
                    }
                });
            });
    }

    // 2. Khởi tạo Biểu đồ User đăng ký mới cho Tab 1
    function loadUsersChart(period) {
        fetch('/admin/dashboard/chart/users?period=' + period)
            .then(response => response.json())
            .then(data => {
                const ctx = document.getElementById('usersChart').getContext('2d');
                if (usersChartInstance) {
                    usersChartInstance.destroy();
                }
                usersChartInstance = new Chart(ctx, {
                    type: 'line',
                    data: {
                        labels: data.labels,
                        datasets: [{
                            label: 'Số lượng đăng ký mới',
                            data: data.data,
                            backgroundColor: 'rgba(22, 160, 133, 0.2)',
                            borderColor: 'rgba(22, 160, 133, 1)',
                            borderWidth: 2,
                            tension: 0.3,
                            fill: true
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        scales: {
                            y: { beginAtZero: true, ticks: { stepSize: 1 } }
                        }
                    }
                });
            });
    }

    // 3. Khởi tạo Biểu đồ so sánh Thu - Chi cho Tab 2 (Tài chính & Ví ảo)
    function loadFinanceChart(period) {
        fetch('/admin/dashboard/chart/finance?period=' + period)
            .then(response => response.json())
            .then(data => {
                const ctx = document.getElementById('financeChart').getContext('2d');
                if (financeChartInstance) {
                    financeChartInstance.destroy();
                }

                financeChartInstance = new Chart(ctx, {
                    type: 'bar',
                    data: {
                        labels: data.labels,
                        datasets: [
                            {
                                label: 'Tổng Thu (Doanh Thu)',
                                data: data.revenues,
                                backgroundColor: 'rgba(46, 204, 113, 0.4)',
                                borderColor: 'rgba(46, 204, 113, 1)',
                                borderWidth: 1.5,
                                borderRadius: 4
                            },
                            {
                                label: 'Tổng Chi (Chi Phí)',
                                data: data.expenses,
                                backgroundColor: 'rgba(231, 76, 60, 0.4)',
                                borderColor: 'rgba(231, 76, 60, 1)',
                                borderWidth: 1.5,
                                borderRadius: 4
                            }
                        ]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        scales: {
                            y: {
                                beginAtZero: true,
                                ticks: {
                                    callback: function(value) {
                                        return value.toLocaleString('vi-VN') + ' đ';
                                    }
                                }
                            }
                        },
                        plugins: {
                            tooltip: {
                                callbacks: {
                                    label: function(context) {
                                        let label = context.dataset.label || '';
                                        if (label) {
                                            label += ': ';
                                        }
                                        if (context.parsed.y !== null) {
                                            label += context.parsed.y.toLocaleString('vi-VN') + ' đ';
                                        }
                                        return label;
                                    }
                                }
                            }
                        }
                    }
                });
            });
    }

    // Tải dữ liệu mặc định ban đầu
    loadRevenueChart('day');
    loadUsersChart('day');
    loadFinanceChart('day');

    // Bắt sự kiện khi lọc thời gian cho biểu đồ Doanh thu/Lợi nhuận Tab 1
    document.getElementById('revenuePeriod').addEventListener('change', function(e) {
        loadRevenueChart(e.target.value);
    });

    // Bắt sự kiện khi lọc thời gian cho biểu đồ Đăng ký mới Tab 1
    document.getElementById('usersPeriod').addEventListener('change', function(e) {
        loadUsersChart(e.target.value);
    });

    // Bắt sự kiện khi lọc thời gian cho biểu đồ Thu - Chi Tab 2
    document.getElementById('financePeriod').addEventListener('change', function(e) {
        loadFinanceChart(e.target.value);
    });

    // Tự động đồng bộ Tab hiện tại lên thanh URL để khi tải lại trang vẫn đúng Tab hoạt động
    const statsTab = document.getElementById('stats-tab');
    const financeTab = document.getElementById('finance-tab');

    if (statsTab && financeTab) {
        statsTab.addEventListener('shown.bs.tab', function() {
            const url = new URL(window.location);
            url.searchParams.set('tab', 'stats');
            window.history.pushState({}, '', url);
        });

        financeTab.addEventListener('shown.bs.tab', function() {
            const url = new URL(window.location);
            url.searchParams.set('tab', 'finance');
            window.history.pushState({}, '', url);
        });
    }
});
