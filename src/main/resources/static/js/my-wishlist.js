document.addEventListener("DOMContentLoaded", () => {
    // Add to cart logic
    document.querySelectorAll('.btn-add-to-cart').forEach(btn => {
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            const bookId = this.getAttribute('data-id');
            const csrfMeta = document.querySelector('meta[name="_csrf"]');
            const csrfToken = csrfMeta ? csrfMeta.getAttribute('content') : '';
            const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
            const csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.getAttribute('content') : '';

            const formData = new URLSearchParams();
            formData.append('bookId', bookId);
            formData.append('quantity', 1);

            const headers = {
                'Content-Type': 'application/x-www-form-urlencoded'
            };
            if (csrfHeader && csrfToken) {
                headers[csrfHeader] = csrfToken;
            }

            const originalHtml = this.innerHTML;
            this.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>...';
            this.disabled = true;

            fetch('/cart/add', {
                method: 'POST',
                headers: headers,
                body: formData.toString()
            })
            .then(res => {
                if (res.redirected && res.url.includes('/login')) {
                    window.location.href = res.url;
                    return null;
                }
                if (res.status === 401) {
                    window.location.href = '/login';
                    return null;
                }
                return res.json();
            })
            .then(data => {
                if (!data) return;
                if (data.success) {
                    let cartBadge = document.getElementById('cartBadge');
                    if (cartBadge) {
                        let currentCount = parseInt(cartBadge.innerText) || 0;
                        cartBadge.innerText = currentCount + 1;
                        cartBadge.style.display = 'inline-block';
                    }

                    if (typeof Swal !== 'undefined') {
                        Swal.fire({
                            icon: 'success',
                            title: 'Thành công',
                            html: data.message || 'Đã thêm vào giỏ hàng!',
                            confirmButtonColor: '#00b074',
                            background: '#181818',
                            color: '#e0e0e0',
                            customClass: {
                                popup: 'border border-success border-opacity-25 rounded-3'
                            }
                        });
                    } else {
                        alert(data.message || 'Đã thêm vào giỏ hàng!');
                    }
                } else {
                    if (typeof Swal !== 'undefined') {
                        Swal.fire({
                            icon: 'error',
                            title: 'Lỗi',
                            html: data.message || 'Thêm vào giỏ hàng thất bại',
                            confirmButtonColor: '#d33',
                            background: '#181818',
                            color: '#e0e0e0',
                            customClass: {
                                popup: 'border border-danger border-opacity-25 rounded-3'
                            }
                        });
                    } else {
                        alert(data.message || 'Thêm vào giỏ hàng thất bại');
                    }
                }
                this.innerHTML = originalHtml;
                this.disabled = false;
            })
            .catch(err => {
                console.error(err);
                if (typeof Swal !== 'undefined') {
                    Swal.fire({
                        icon: 'error',
                        title: 'Lỗi',
                        html: 'Không thể thêm vào giỏ hàng do lỗi mạng.',
                        confirmButtonColor: '#d33',
                        background: '#181818',
                        color: '#e0e0e0'
                    });
                } else {
                    alert('Lỗi mạng khi thêm vào giỏ hàng.');
                }
                this.innerHTML = originalHtml;
                this.disabled = false;
            });
        });
    });
    
    // Convert default delete prompt to SweetAlert2 for consistency
    document.querySelectorAll('.btn-delete-wishlist').forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const form = this.closest('form');
            if (typeof Swal !== 'undefined') {
                Swal.fire({
                    title: 'Xóa khỏi yêu thích?',
                    text: 'Bạn có chắc muốn xóa truyện này khỏi danh sách yêu thích?',
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
                    if (result.isConfirmed) form.submit();
                });
            } else {
                if (confirm('Xóa khỏi yêu thích?')) form.submit();
            }
        });
    });

    // Clear all wishlist items
    const clearAllBtn = document.querySelector('.btn-clear-all');
    if (clearAllBtn) {
        clearAllBtn.addEventListener('click', function(e) {
            e.preventDefault();
            const form = this.closest('form');
            if (typeof Swal !== 'undefined') {
                Swal.fire({
                    title: 'Xóa tất cả yêu thích?',
                    text: 'Hành động này sẽ dọn trống hoàn toàn danh sách yêu thích của bạn!',
                    icon: 'warning',
                    showCancelButton: true,
                    confirmButtonColor: '#dc3545',
                    cancelButtonColor: '#6c757d',
                    confirmButtonText: 'Xóa tất cả',
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
                if (confirm('Bạn có chắc chắn muốn xóa tất cả truyện khỏi danh sách yêu thích?')) {
                    form.submit();
                }
            }
        });
    }
});
