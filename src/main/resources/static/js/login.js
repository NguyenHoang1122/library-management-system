document.addEventListener("DOMContentLoaded", function() {
    if (document.getElementById('bannedErrorMarker')) {
        Swal.fire({
            icon: 'warning',
            title: 'Tài khoản đã bị khóa',
            html: 'Vui lòng liên hệ với admin để được hỗ trợ.<br>SĐT: <b>0999999999</b>',
            confirmButtonText: 'Đóng',
            confirmButtonColor: '#dc3545',
            background: '#181818',
            color: '#e0e0e0',
            customClass: {
                popup: 'border border-danger border-opacity-25 rounded-3'
            }
        });
    }
});
