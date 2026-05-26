document.addEventListener("DOMContentLoaded", function() {
    const csrfMeta = document.querySelector('meta[name="_csrf"]');
    const csrfToken = csrfMeta ? csrfMeta.getAttribute('content') : '';
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
    const csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.getAttribute('content') : '';

    function updateQuantity(bookId, quantity) {
        const formData = new URLSearchParams();
        formData.append('bookId', bookId);
        formData.append('quantity', quantity);
        
        const headers = {
            'Content-Type': 'application/x-www-form-urlencoded'
        };
        if (csrfHeader && csrfToken) {
            headers[csrfHeader] = csrfToken;
        }

        fetch('/cart/update', {
            method: 'POST',
            headers: headers,
            body: formData.toString()
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                location.reload();
            } else {
                Swal.fire({
                    icon: 'error',
                    title: 'Lỗi',
                    html: data.message || 'Không thể cập nhật số lượng',
                    confirmButtonColor: '#d33',
                    background: '#181818',
                    color: '#e0e0e0',
                    customClass: {
                        popup: 'border border-danger border-opacity-25 rounded-3'
                    }
                });
            }
        })
        .catch(err => console.error('Error:', err));
    }

    document.querySelectorAll('.btn-decrease').forEach(btn => {
        btn.addEventListener('click', function() {
            let input = this.nextElementSibling;
            let val = parseInt(input.value);
            if (val > 1) {
                updateQuantity(this.dataset.id, val - 1);
            }
        });
    });

    document.querySelectorAll('.btn-increase').forEach(btn => {
        btn.addEventListener('click', function() {
            let input = this.previousElementSibling;
            let val = parseInt(input.value);
            let max = parseInt(input.getAttribute('max'));
            if (val < max) {
                updateQuantity(this.dataset.id, val + 1);
            } else {
                Swal.fire({
                    icon: 'warning',
                    title: 'Thông báo',
                    html: 'Không thể mượn quá số lượng trong kho!',
                    confirmButtonColor: '#f39c12',
                    background: '#181818',
                    color: '#e0e0e0',
                    customClass: {
                        popup: 'border border-warning border-opacity-25 rounded-3'
                    }
                });
            }
        });
    });

    document.querySelectorAll('.quantity-input').forEach(input => {
        input.addEventListener('change', function() {
            let val = parseInt(this.value);
            let max = parseInt(this.getAttribute('max'));
            
            if (isNaN(val) || val < 1) {
                val = 1;
                this.value = val;
                updateQuantity(this.dataset.id, val);
            } else if (val > max) {
                val = max;
                this.value = val;
                Swal.fire({
                    icon: 'warning',
                    title: 'Thông báo',
                    html: 'Không thể mượn quá số lượng trong kho!',
                    confirmButtonColor: '#f39c12',
                    background: '#181818',
                    color: '#e0e0e0',
                    customClass: {
                        popup: 'border border-warning border-opacity-25 rounded-3'
                    }
                }).then(() => {
                    updateQuantity(this.dataset.id, val);
                });
            } else {
                updateQuantity(this.dataset.id, val);
            }
        });
    });

    // Handle address required error redirect from CheckoutController
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('error') === 'address_required') {
        Swal.fire({
            icon: 'warning',
            title: 'Chưa cập nhật địa chỉ',
            html: 'Bạn cần cập nhật địa chỉ giao hàng trước khi tiến hành thanh toán.',
            confirmButtonText: 'Đồng ý',
            confirmButtonColor: '#198754',
            background: '#181818',
            color: '#e0e0e0',
            allowOutsideClick: false,
            customClass: {
                popup: 'border border-warning border-opacity-25 rounded-3'
            }
        }).then((result) => {
            if (result.isConfirmed) {
                window.location.href = '/user/profile';
            }
        });
    }
});
