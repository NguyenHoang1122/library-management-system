function updateHeaderBalance(newBalance) {
    const balanceEl = document.getElementById('headerWalletBalance');
    if (balanceEl) {
        balanceEl.innerText = new Intl.NumberFormat('vi-VN').format(newBalance) + ' đ';
    }
    // Also update profile balance if it exists
    const profileBalanceEl = document.getElementById('walletBalanceDisplay');
    if (profileBalanceEl) {
        profileBalanceEl.innerText = new Intl.NumberFormat('vi-VN').format(newBalance) + ' đ';
    }
}

function getCsrfToken() {
    const meta = document.querySelector('meta[name="_csrf"]');
    return meta ? meta.getAttribute('content') : '';
}

function getCsrfHeader() {
    const meta = document.querySelector('meta[name="_csrf_header"]');
    return meta ? meta.getAttribute('content') : '';
}

function handleWalletTransaction(actionStr, url) {
    Swal.fire({
        title: actionStr + ' tiền',
        input: 'number',
        inputLabel: 'Nhập số tiền muốn ' + actionStr.toLowerCase() + ' (VNĐ)',
        inputPlaceholder: 'Ví dụ: 100000',
        inputAttributes: {
            min: 10000,
            step: 10000
        },
        showCancelButton: true,
        confirmButtonText: 'Xác nhận',
        cancelButtonText: 'Hủy',
        confirmButtonColor: actionStr === 'Nạp' ? '#198754' : '#ffc107',
        background: '#181818',
        color: '#e0e0e0',
        inputValidator: (value) => {
            if (!value || value < 10000) {
                return 'Số tiền phải từ 10.000đ trở lên!';
            }
        }
    }).then((result) => {
        if (result.isConfirmed) {
            const amount = result.value;
            const formData = new URLSearchParams();
            formData.append('amount', amount);
            
            const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
            const csrfHeader = getCsrfHeader();
            const csrfToken = getCsrfToken();
            if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

            Swal.showLoading();

            fetch(url, {
                method: 'POST',
                headers: headers,
                body: formData.toString()
            })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    updateHeaderBalance(data.newBalance);
                    Swal.fire({
                        icon: 'success',
                        title: 'Thành công',
                        text: `Đã ${actionStr.toLowerCase()} ${new Intl.NumberFormat('vi-VN').format(amount)}đ thành công!`,
                        background: '#181818',
                        color: '#e0e0e0',
                        timer: 2000,
                        showConfirmButton: false
                    });
                } else {
                    Swal.fire('Lỗi', data.message || 'Có lỗi xảy ra', 'error');
                }
            })
            .catch(err => {
                console.error(err);
                Swal.fire('Lỗi', 'Không thể kết nối đến máy chủ', 'error');
            });
        }
    });
}

function openHeaderDeposit() {
    handleWalletTransaction('Nạp', '/user/deposit');
}

function openHeaderWithdraw() {
    handleWalletTransaction('Rút', '/user/withdraw');
}
