function confirmReturn(button) {
    const row = button.closest('tr');
    const isOverdue = row.classList.contains('row-overdue');
    const message = isOverdue ? 'Truyện đã QUÁ HẠN! Xác nhận trả và tính phí phạt?' : 'Xác nhận xử lý trả truyện trực tiếp (không qua yêu cầu)?';
    return confirm(message);
}

function openRejectModal(requestId) {
    const modal = new bootstrap.Modal(document.getElementById('rejectReturnModal'));
    document.getElementById('rejectReturnForm').action = '/librarian/returns/reject/' + requestId;
    modal.show();
}
