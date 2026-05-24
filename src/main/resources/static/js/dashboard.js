document.addEventListener("DOMContentLoaded", function() {
    Chart.defaults.color = '#ccc'; // Dark theme default text color
    Chart.defaults.borderColor = '#333'; // Dark theme grid line color

    let revChartInstance = null;
    let usersChartInstance = null;

    function loadRevenueChart(period) {
        fetch('/admin/dashboard/chart/revenue?period=' + period)
            .then(response => response.json())
            .then(data => {
                const ctx = document.getElementById('revenueChart').getContext('2d');
                if (revChartInstance) revChartInstance.destroy();
                revChartInstance = new Chart(ctx, {
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
                if (usersChartInstance) usersChartInstance.destroy();
                usersChartInstance = new Chart(ctx, {
                    type: 'line',
                    data: {
                        labels: data.labels,
                        datasets: [{
                            label: 'Số lượng đăng ký mới',
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
                        }
                    }
                });
            });
    }

    // Initialize with default 'day'
    loadRevenueChart('day');
    loadUsersChart('day');

    // Bind events
    document.getElementById('revenuePeriod').addEventListener('change', function(e) {
        loadRevenueChart(e.target.value);
    });

    document.getElementById('usersPeriod').addEventListener('change', function(e) {
        loadUsersChart(e.target.value);
    });
});
