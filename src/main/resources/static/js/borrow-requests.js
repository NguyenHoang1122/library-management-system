document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.btn-cancel-request').forEach(button => {
        button.addEventListener('click', function(e) {
            const form = this.closest('form');
            if (typeof Swal !== 'undefined') {
                Swal.fire({
                    title: 'Xác nhận hủy?',
                    text: 'Bạn có chắc chắn muốn hủy yêu cầu mượn truyện này không?',
                    icon: 'warning',
                    showCancelButton: true,
                    confirmButtonColor: '#dc3545',
                    cancelButtonColor: '#6c757d',
                    confirmButtonText: 'Đồng ý hủy',
                    cancelButtonText: 'Không, giữ lại',
                    background: '#181818',
                    color: '#e0e0e0',
                    customClass: {
                        popup: 'border border-danger border-opacity-25 rounded-3'
                    }
                }).then((result) => {
                    if (result.isConfirmed) {
                        form.submit();
                    }
                });
            } else {
                if (confirm('Bạn có chắc chắn muốn hủy yêu cầu mượn truyện này không?')) {
                    form.submit();
                }
            }
        });
    });
});
