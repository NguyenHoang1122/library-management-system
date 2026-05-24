document.querySelectorAll('.btn-edit-category').forEach(button => {
    button.addEventListener('click', function() {
        const id = this.getAttribute('data-id');
        const name = this.getAttribute('data-name');
        
        // Cập nhật action của form và giá trị input
        document.getElementById('editCategoryForm').action = '/categories/update/' + id;
        document.getElementById('editCategoryName').value = name;
        
        // Hiển thị modal
        const editModal = new bootstrap.Modal(document.getElementById('editCategoryModal'));
        editModal.show();
    });
});

// SweetAlert2 delete confirmation for categories
document.querySelectorAll('.btn-delete-category').forEach(button => {
    button.addEventListener('click', function(e) {
        e.preventDefault();
        const deleteUrl = this.getAttribute('href');
        
        if (typeof Swal !== 'undefined') {
            Swal.fire({
                title: 'Xác nhận xóa?',
                text: "Bạn có chắc chắn muốn xóa danh mục này không? Các truyện thuộc danh mục này sẽ mất liên kết danh mục tương ứng!",
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
                    window.location.href = deleteUrl;
                }
            });
        } else {
            if (confirm('Bạn có chắc chắn muốn xóa danh mục này không?')) {
                window.location.href = deleteUrl;
            }
        }
    });
});

