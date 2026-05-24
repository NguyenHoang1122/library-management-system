function openRejectModal(requestId) {
    const modal = new bootstrap.Modal(document.getElementById('rejectReturnModal'));
    document.getElementById('rejectReturnForm').action = '/librarian/returns/reject/' + requestId;
    modal.show();
}

function openConfirmReturnModal(requestId) {
    const modal = new bootstrap.Modal(document.getElementById('confirmReturnModal'));
    document.getElementById('confirmReturnForm').action = '/librarian/returns/complete/' + requestId;
    modal.show();
}
