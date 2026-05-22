function confirmCompleteDelivery(requestId) {
    Swal.fire({
        title: 'Hoàn thành đơn?',
        text: 'Xác nhận rằng người dùng đã nhận đủ sách. Đơn sẽ chuyển sang trạng thái Đang mượn.',
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#198754',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Đồng ý',
        cancelButtonText: 'Hủy',
        background: '#181818',
        color: '#e0e0e0'
    }).then((result) => {
        if (result.isConfirmed) {
            document.getElementById('completeDeliveryForm').submit();
        }
    });
}

function confirmRejectDelivery(requestId) {
    Swal.fire({
        title: 'Hủy đơn giao',
        input: 'textarea',
        inputLabel: 'Lý do hủy đơn',
        inputPlaceholder: 'Nhập lý do tại đây...',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Xác nhận hủy',
        cancelButtonText: 'Đóng',
        background: '#181818',
        color: '#e0e0e0'
    }).then((result) => {
        if (result.isConfirmed) {
            const form = document.getElementById('rejectDeliveryForm');
            const reasonInput = document.getElementById('rejectReasonInput');
            reasonInput.value = result.value;
            form.submit();
        }
    });
}
