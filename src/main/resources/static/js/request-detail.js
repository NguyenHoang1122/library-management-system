function confirmCancel() {
    Swal.fire({
        title: 'Hủy Đơn Mượn?',
        text: 'Bạn có chắc chắn muốn hủy đơn mượn này? Tiền thanh toán sẽ được hoàn lại đầy đủ vào ví của bạn.',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Đồng ý hủy',
        cancelButtonText: 'Không',
        background: '#181818',
        color: '#e0e0e0'
    }).then((result) => {
        if (result.isConfirmed) {
            document.getElementById('cancelRequestForm').submit();
        }
    });
}

function toggleReturnShipping() {
    const isShipping = document.getElementById('returnShipping').checked;
    const box = document.getElementById('shippingInfoBox');
    if (isShipping) {
        box.classList.remove('d-none');
        // Mock tính phí
        setTimeout(() => {
            document.getElementById('returnShippingFee').innerHTML = '25,000 đ';
        }, 500);
    } else {
        box.classList.add('d-none');
    }
}

function showExtendDialog() {
    Swal.fire({
        title: 'Gia hạn sách?',
        html: '<p class="mb-3 text-secondary">Bạn muốn gia hạn thêm <strong class="text-warning">7 ngày</strong> cho các truyện đang mượn?</p>' +
              '<div class="p-3 bg-black rounded border border-secondary text-start">' +
              '<div class="d-flex justify-content-between mb-2"><span class="text-secondary">Phí gia hạn:</span><span class="fw-bold text-light">10,000 đ</span></div>' +
              '<div class="d-flex justify-content-between"><span class="text-secondary">Hạn trả mới dự kiến:</span><span class="fw-bold text-info">Cộng thêm 7 ngày</span></div>' +
              '</div>',
        icon: 'question',
        background: '#1a1a1a',
        color: '#fff',
        showCancelButton: true,
        confirmButtonColor: '#0dcaf0',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Đồng ý Gia hạn',
        cancelButtonText: 'Hủy bỏ'
    }).then((result) => {
        if (result.isConfirmed) {
            document.getElementById('extendRequestForm').submit();
        }
    });
}
