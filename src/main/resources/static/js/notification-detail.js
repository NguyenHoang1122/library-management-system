document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.btn-delete-detail').forEach(button => {
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
                if (confirm('Bạn có chắc chắn muốn xóa thông báo này?')) {
                    form.submit();
                }
            }
        });
    });
});
