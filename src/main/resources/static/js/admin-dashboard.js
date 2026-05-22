document.addEventListener("DOMContentLoaded", function() {
    Chart.defaults.color = '#aaa';
    Chart.defaults.scale.grid.color = 'rgba(255, 255, 255, 0.1)';

    let revenueChartInstance = null;
    let usersChartInstance = null;

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
                            label: 'Doanh thu (VNĐ)',
                            data: data.data,
                            backgroundColor: 'rgba(54, 162, 235, 0.6)',
                            borderColor: 'rgba(54, 162, 235, 1)',
                            borderWidth: 1,
                            borderRadius: 4
                        }]
                    },
                    options: {
                        responsive: true,
                        scales: {
                            y: { beginAtZero: true }
                        },
                        plugins: {
                            legend: {
                                labels: { color: '#e0e0e0' }
                            }
                        }
                    }
                });
            });
    }

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
                            label: 'Số lượng đăng ký',
                            data: data.data,
                            backgroundColor: 'rgba(75, 192, 192, 0.2)',
                            borderColor: 'rgba(75, 192, 192, 1)',
                            borderWidth: 2,
                            tension: 0.3,
                            fill: true
                        }]
                    },
                    options: {
                        responsive: true,
                        scales: {
                            y: { beginAtZero: true, ticks: { stepSize: 1 } }
                        },
                        plugins: {
                            legend: {
                                labels: { color: '#e0e0e0' }
                            }
                        }
                    }
                });
            });
    }

    // Initial load
    loadRevenueChart('day');
    loadUsersChart('day');

    // Event listeners for dropdowns
    document.getElementById('revenuePeriod').addEventListener('change', function() {
        loadRevenueChart(this.value);
    });

    document.getElementById('usersPeriod').addEventListener('change', function() {
        loadUsersChart(this.value);
    });
});
