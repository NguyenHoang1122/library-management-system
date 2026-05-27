/* Javascript dành riêng cho Thống kê Nghiệp vụ của Thủ thư (Librarian Dashboard) */

document.addEventListener("DOMContentLoaded", function() {
    const chartCanvas = document.getElementById('inventoryChart');
    if (!chartCanvas) return;

    // Đọc dữ liệu từ data attributes trên canvas để đảm bảo tách biệt mã HTML và JS
    const booksInWarehouse = parseInt(chartCanvas.getAttribute('data-warehouse')) || 0;
    const booksLentOut = parseInt(chartCanvas.getAttribute('data-lent')) || 0;

    const ctx = chartCanvas.getContext('2d');
    new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Sách Trong Kho', 'Sách Đang Mượn'],
            datasets: [{
                data: [booksInWarehouse, booksLentOut],
                backgroundColor: [
                    'rgba(46, 204, 113, 0.75)', // Màu xanh lá cho sách trong kho
                    'rgba(52, 152, 219, 0.75)'  // Màu xanh dương cho sách mượn
                ],
                borderColor: [
                    '#2ecc71',
                    '#3498db'
                ],
                borderWidth: 1.5
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false // Ẩn nhãn mặc định để dùng chú thích custom
                }
            },
            cutout: '70%' // Tạo hiệu ứng vòng tròn thanh thoát
        }
    });
});
