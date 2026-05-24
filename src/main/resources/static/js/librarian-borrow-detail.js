function confirmApprove(requestId) {
    Swal.fire({
        title: 'Duyệt đơn mượn?',
        text: 'Đơn này sẽ được duyệt với thời hạn mặc định là 7 ngày.',
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#198754',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Đồng ý duyệt',
        cancelButtonText: 'Hủy',
        background: '#181818',
        color: '#e0e0e0'
    }).then((result) => {
        if (result.isConfirmed) {
            document.getElementById('approveRequestForm').submit();
        }
    });
}

function confirmReject(requestId) {
    Swal.fire({
        title: 'Từ chối đơn mượn',
        input: 'textarea',
        inputLabel: 'Lý do từ chối (Tùy chọn)',
        inputPlaceholder: 'Nhập lý do tại đây...',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Xác nhận từ chối',
        cancelButtonText: 'Hủy',
        background: '#181818',
        color: '#e0e0e0',
        inputValidator: (value) => {
            // Optional validator if needed
        }
    }).then((result) => {
        if (result.isConfirmed) {
            const form = document.getElementById('rejectRequestForm');
            const reasonInput = document.getElementById('rejectReasonInput');
            reasonInput.value = result.value;
            form.submit();
        }
    });
}
