document.getElementById('saveRoleChanges').addEventListener('click', function () {
    const selects = document.querySelectorAll('.role-select');
    let hasChanges = false;
    selects.forEach(select => {
        const userId = select.getAttribute('data-user-id');
        const newRole = select.value;
        hasChanges = true;
        const form = document.createElement('form');
        form.method = 'post';
        form.action = `/user/${userId}/change-role`;
        const input = document.createElement('input');
        input.type = 'hidden'; input.name = 'roleName'; input.value = newRole;
        form.appendChild(input);
        document.body.appendChild(form);
        form.submit();
    });

    if (!hasChanges) alert('Không có thay đổi nào.');
});

// SweetAlert2 delete confirmation for users
document.querySelectorAll('.btn-delete-user').forEach(button => {
    button.addEventListener('click', function(e) {
        e.preventDefault();
        const form = this.closest('form');
        
        if (typeof Swal !== 'undefined') {
            Swal.fire({
                title: 'Xác nhận xóa người dùng?',
                text: "Tài khoản của người dùng này sẽ bị tạm khóa và chuyển vào thùng rác!",
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
            if (confirm('Bạn có chắc chắn muốn xóa người dùng này?')) {
                form.submit();
            }
        }
    });
});

