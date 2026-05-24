document.addEventListener('DOMContentLoaded', () => {
    // Delete read notifications
    document.querySelectorAll('.btn-delete-read-page').forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const form = this.closest('form');
            
            // Check if there are read notifications visible in the view
            const hasReadNoti = document.querySelectorAll('.noti-item-waka:not(.unread)').length > 0;
            if (!hasReadNoti) {
                if (typeof Swal !== 'undefined') {
                    Swal.fire({
                        icon: 'info',
                        title: 'Thông báo',
                        text: 'Không có thông báo nào đã đọc để xóa!',
                        confirmButtonColor: '#00b074',
                        background: '#181818',
                        color: '#e0e0e0',
                        customClass: {
                            popup: 'border border-info border-opacity-25 rounded-3'
                        }
                    });
                } else {
                    alert('Không có thông báo nào đã đọc để xóa!');
                }
                return;
            }
            
            if (typeof Swal !== 'undefined') {
                Swal.fire({
                    title: 'Xác nhận xóa?',
                    text: "Bạn có chắc chắn muốn xóa tất cả thông báo đã đọc?",
                    icon: 'warning',
                    showCancelButton: true,
                    confirmButtonColor: '#dc3545',
                    cancelButtonColor: '#6c757d',
                    confirmButtonText: 'Xóa ngay',
                    cancelButtonText: 'Hủy',
                    background: '#181818',
                    color: '#e0e0e0',
                    customClass: {
                        popup: 'border border-danger border-opacity-25 rounded-3',
                        title: 'text-danger fw-bold',
                        confirmButton: 'btn btn-danger px-4 py-2 rounded-pill',
                        cancelButton: 'btn btn-outline-secondary px-4 py-2 rounded-pill ms-2'
                    },
                    buttonsStyling: false
                }).then((result) => {
                    if (result.isConfirmed) {
                        form.submit();
                    }
                });
            } else {
                if (confirm('Bạn có chắc chắn muốn xóa tất cả thông báo đã đọc?')) {
                    form.submit();
                }
            }
        });
    });

    // Delete single notification
    document.querySelectorAll('.btn-delete-single').forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const form = this.closest('form');
            
            if (typeof Swal !== 'undefined') {
                Swal.fire({
                    title: 'Xác nhận xóa?',
                    text: "Bạn có chắc chắn muốn xóa thông báo này?",
                    icon: 'warning',
                    showCancelButton: true,
                    confirmButtonColor: '#dc3545',
                    cancelButtonColor: '#6c757d',
                    confirmButtonText: 'Xóa',
                    cancelButtonText: 'Hủy',
                    background: '#181818',
                    color: '#e0e0e0',
                    customClass: {
                        popup: 'border border-danger border-opacity-25 rounded-3',
                        title: 'text-danger fw-bold',
                        confirmButton: 'btn btn-danger px-4 py-2 rounded-pill',
                        cancelButton: 'btn btn-outline-secondary px-4 py-2 rounded-pill ms-2'
                    },
                    buttonsStyling: false
                }).then((result) => {
                    if (result.isConfirmed) {
                        form.submit();
                    }
                });
            } else {
                if (confirm('Bạn có chắc chắn muốn xóa thông báo này?')) {
                    form.submit();
                }
            }
        });
    });
});
