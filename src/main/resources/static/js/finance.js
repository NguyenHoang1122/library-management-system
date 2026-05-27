document.addEventListener("DOMContentLoaded", function() {
    // Thiết lập màu sắc và viền mặc định cho biểu đồ theo giao diện tối
    Chart.defaults.color = '#888';
    Chart.defaults.borderColor = '#2d2d2d';

    let financeChartInstance = null;

    // Hàm gọi API lấy dữ liệu biểu đồ và vẽ bằng Chart.js
    function loadFinanceChart(period) {
        fetch('/admin/finance/chart?period=' + period)
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

    // Khởi tạo biểu đồ mặc định hiển thị theo ngày
    loadFinanceChart('day');

    // Bắt sự kiện khi thay đổi bộ lọc thời gian
    document.getElementById('financePeriod').addEventListener('change', function(e) {
        loadFinanceChart(e.target.value);
    });
});
