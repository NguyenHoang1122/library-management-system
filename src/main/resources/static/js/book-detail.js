const wishlistBtn = document.getElementById('wishlistBtn');
const borrowBtn = document.getElementById('borrowBtn');

if (wishlistBtn) {
    const bookId = wishlistBtn.getAttribute('data-book-id');
    wishlistBtn.addEventListener('click', () => addToWishlist(bookId));
    checkWishlistStatus(bookId);
}

// Xử lý nút Thêm vào giỏ hàng
const btnAddToCart = document.querySelector('.btn-add-to-cart');
if (btnAddToCart) {
    btnAddToCart.addEventListener('click', function(e) {
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
        this.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> Đang thêm...';
        this.disabled = true;

        fetch('/cart/add', {
            method: 'POST',
            headers: headers,
            body: formData.toString()
        })
        .then(res => {
            // Check if redirect to login
            if (res.redirected && res.url.includes('/login')) {
                window.location.href = res.url;
                return null; // Stop chain
            }
            if (res.status === 401) {
                window.location.href = '/login';
                return null;
            }
            return res.json();
        })
        .then(data => {
            if (!data) return; // redirected

            if (data.success) {
                let cartBadge = document.getElementById('cartBadge');
                if (cartBadge) {
                    let currentCount = parseInt(cartBadge.innerText) || 0;
                    cartBadge.innerText = currentCount + 1;
                    cartBadge.style.display = 'inline-block';
                }

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
            }
            this.innerHTML = originalHtml;
            this.disabled = false;
        })
        .catch(err => {
            console.error('Error adding to cart:', err);
            Swal.fire({
                icon: 'error',
                title: 'Lỗi',
                html: 'Không thể thêm vào giỏ hàng do lỗi mạng.',
                confirmButtonColor: '#d33',
                background: '#181818',
                color: '#e0e0e0',
                customClass: {
                    popup: 'border border-danger border-opacity-25 rounded-3'
                }
            });
            this.innerHTML = originalHtml;
            this.disabled = false;
        });
    });
}



function addToWishlist(bookId) {
    fetch(`/wishlist/add/${bookId}`, { method: 'POST' })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                if (data.added) wishlistBtn.classList.add('active');
                else wishlistBtn.classList.remove('active');
            }
        });
}

function checkWishlistStatus(bookId) {
    fetch(`/wishlist/check/${bookId}`)
        .then(res => res.json())
        .then(data => {
            if (data.inWishlist) wishlistBtn.classList.add('active');
        });
}

// Interactive rating stars selection
const starSelects = document.querySelectorAll('.star-select');
const selectedRatingInput = document.getElementById('selectedRating');

if (starSelects && starSelects.length > 0) {
    let currentRating = parseInt(selectedRatingInput.value) || 5;

    function updateStars(rating) {
        starSelects.forEach((star, index) => {
            if (index < rating) {
                star.classList.replace('bi-star', 'bi-star-fill');
                star.classList.replace('text-secondary', 'text-warning');
                star.style.color = '#f39c12';
            } else {
                star.classList.replace('bi-star-fill', 'bi-star');
                star.classList.replace('text-warning', 'text-secondary');
                star.style.color = '#ccc';
            }
        });
    }

    // Initialize stars state on page load
    updateStars(currentRating);

    starSelects.forEach(star => {
        star.addEventListener('mouseover', () => {
            const val = parseInt(star.getAttribute('data-value'));
            updateStars(val);
        });

        star.addEventListener('click', () => {
            currentRating = parseInt(star.getAttribute('data-value'));
            selectedRatingInput.value = currentRating;
            updateStars(currentRating);
        });
    });

    const starContainer = document.querySelector('.star-rating-select');
    if (starContainer) {
        starContainer.addEventListener('mouseleave', () => {
            updateStars(currentRating);
        });
    }
}

// Tab switching logic
const tabRating = document.getElementById('tab-rating');
const tabComment = document.getElementById('tab-comment');
const ratingContent = document.getElementById('rating-content');
const commentContent = document.getElementById('comment-content');

if (tabRating && tabComment) {
    // Check url for commentPage to default to comment tab
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('commentPage')) {
        sessionStorage.setItem('activeTab', 'comment');
    }

    const activeTab = sessionStorage.getItem('activeTab') || 'rating';
    
    function switchTab(tab) {
        if (tab === 'comment') {
            tabComment.classList.add('active');
            tabComment.style.borderBottomColor = '#00d2d3';
            tabComment.style.color = '#00d2d3';
            
            tabRating.classList.remove('active');
            tabRating.style.borderBottomColor = 'transparent';
            tabRating.style.color = '#aaa';

            commentContent.style.display = 'block';
            ratingContent.style.display = 'none';
            sessionStorage.setItem('activeTab', 'comment');
        } else {
            tabRating.classList.add('active');
            tabRating.style.borderBottomColor = '#00d2d3';
            tabRating.style.color = '#00d2d3';
            
            tabComment.classList.remove('active');
            tabComment.style.borderBottomColor = 'transparent';
            tabComment.style.color = '#aaa';

            ratingContent.style.display = 'block';
            commentContent.style.display = 'none';
            sessionStorage.setItem('activeTab', 'rating');
        }
    }

    switchTab(activeTab);

    tabRating.addEventListener('click', () => switchTab('rating'));
    tabComment.addEventListener('click', () => switchTab('comment'));
}

// Submit rating form via AJAX
const ratingForm = document.getElementById('ratingForm');
if (ratingForm) {
    ratingForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const bookId = ratingForm.getAttribute('data-book-id');
        const rating = selectedRatingInput.value;
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

        const submitBtn = document.getElementById('submitRatingBtn');
        submitBtn.disabled = true;
        const originalText = submitBtn.innerText;
        submitBtn.innerText = 'Đang gửi...';

        fetch(`/books/${bookId}/rating`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                [csrfHeader]: csrfToken
            },
            body: `rating=${rating}`
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                showSuccessModal(data.message);
            } else {
                alert("Lỗi: " + data.message);
                submitBtn.disabled = false;
                submitBtn.innerText = originalText;
            }
        })
        .catch(err => {
            console.error(err);
            alert("Đã xảy ra lỗi khi gửi đánh giá.");
            submitBtn.disabled = false;
            submitBtn.innerText = originalText;
        });
    });
}

// Submit comment form via AJAX
const commentForm = document.getElementById('commentForm');
if (commentForm) {
    commentForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const bookId = commentForm.getAttribute('data-book-id');
        const content = document.getElementById('commentContent').value;
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

        const submitBtn = document.getElementById('submitCommentBtn');
        submitBtn.disabled = true;
        const originalText = submitBtn.innerText;
        submitBtn.innerText = 'Đang gửi...';

        fetch(`/books/${bookId}/comment`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                [csrfHeader]: csrfToken
            },
            body: `content=${encodeURIComponent(content)}`
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                showSuccessModal(data.message);
            } else {
                alert("Lỗi: " + data.message);
                submitBtn.disabled = false;
                submitBtn.innerText = originalText;
            }
        })
        .catch(err => {
            console.error(err);
            alert("Đã xảy ra lỗi khi gửi bình luận.");
            submitBtn.disabled = false;
            submitBtn.innerText = originalText;
        });
    });
}

// Hide comment
document.querySelectorAll('.btn-hide-comment').forEach(btn => {
    btn.addEventListener('click', function() {
        const commentId = this.getAttribute('data-id');
        Swal.fire({
            title: 'Ẩn bình luận?',
            text: 'Bạn có chắc muốn ẩn bình luận này khỏi người dùng?',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#ffc107',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Đồng ý ẩn',
            cancelButtonText: 'Hủy bỏ',
            background: '#181818',
            color: '#e0e0e0',
            customClass: {
                popup: 'border border-warning border-opacity-25 rounded-3'
            }
        }).then((result) => {
            if (result.isConfirmed) {
                const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
                const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

                fetch(`/books/comments/${commentId}/hide`, {
                    method: 'POST',
                    headers: {
                        [csrfHeader]: csrfToken
                    }
                })
                .then(res => res.json())
                .then(data => {
                    if (data.success) {
                        location.reload();
                    } else {
                        Swal.fire({ icon: 'error', title: 'Lỗi', text: data.message, background: '#181818', color: '#e0e0e0' });
                    }
                });
            }
        });
    });
});

// Edit comment by User using Bootstrap Modal
document.querySelectorAll('.edit-comment-btn').forEach(btn => {
    btn.addEventListener('click', function() {
        const commentId = this.getAttribute('data-id');
        const contentElem = document.getElementById('comment-content-' + commentId);
        const oldContent = contentElem ? contentElem.innerText : '';
        
        document.getElementById('editCommentId').value = commentId;
        document.getElementById('editCommentContent').value = oldContent;
        
        const editModal = new bootstrap.Modal(document.getElementById('editCommentModal'));
        editModal.show();
    });
});

document.getElementById('submitEditCommentBtn')?.addEventListener('click', function() {
    const commentId = document.getElementById('editCommentId').value;
    const newContent = document.getElementById('editCommentContent').value;
    
    if (!newContent || newContent.trim() === '') {
        alert('Nội dung bình luận không được để trống!');
        return;
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    const formData = new URLSearchParams();
    formData.append('content', newContent);

    this.disabled = true;
    this.innerText = 'Đang lưu...';

    fetch(`/books/comments/${commentId}/edit`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            [csrfHeader]: csrfToken
        },
        body: formData.toString()
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            const editModalEl = document.getElementById('editCommentModal');
            const editModal = bootstrap.Modal.getInstance(editModalEl);
            editModal.hide();
            showSuccessModal(data.message);
        } else {
            alert("Lỗi: " + data.message);
            this.disabled = false;
            this.innerText = 'Lưu thay đổi';
        }
    });
});

// Delete comment by User using Bootstrap Modal
document.querySelectorAll('.user-delete-comment-btn').forEach(btn => {
    btn.addEventListener('click', function() {
        const commentId = this.getAttribute('data-id');
        document.getElementById('deleteUserCommentId').value = commentId;
        
        const deleteModal = new bootstrap.Modal(document.getElementById('deleteCommentConfirmModal'));
        deleteModal.show();
    });
});

document.getElementById('confirmUserDeleteCommentBtn')?.addEventListener('click', function() {
    const commentId = document.getElementById('deleteUserCommentId').value;
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    this.disabled = true;
    this.innerText = 'Đang xóa...';

    fetch(`/books/comments/${commentId}/user-delete`, {
        method: 'POST',
        headers: {
            [csrfHeader]: csrfToken
        }
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            const deleteModalEl = document.getElementById('deleteCommentConfirmModal');
            const deleteModal = bootstrap.Modal.getInstance(deleteModalEl);
            deleteModal.hide();
            showSuccessModal(data.message);
        } else {
            alert("Lỗi: " + data.message);
            this.disabled = false;
            this.innerText = 'Xóa bình luận';
        }
    });
});

// Admin Delete Comment using Bootstrap Modal
document.querySelectorAll('.admin-delete-comment-btn').forEach(btn => {
    btn.addEventListener('click', function() {
        const commentId = this.getAttribute('data-id');
        document.getElementById('adminDeleteCommentId').value = commentId;
        document.getElementById('adminDeleteReason').value = '';
        
        const adminDeleteModal = new bootstrap.Modal(document.getElementById('adminDeleteCommentModal'));
        adminDeleteModal.show();
    });
});

document.getElementById('confirmAdminDeleteCommentBtn')?.addEventListener('click', function() {
    const commentId = document.getElementById('adminDeleteCommentId').value;
    const reason = document.getElementById('adminDeleteReason').value;
    
    if (!reason || reason.trim() === '') {
        alert('Vui lòng nhập lý do xóa!');
        return;
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    
    const formData = new URLSearchParams();
    formData.append('reason', reason);

    this.disabled = true;
    this.innerText = 'Đang xóa...';

    fetch(`/books/comments/${commentId}/delete`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            [csrfHeader]: csrfToken
        },
        body: formData.toString()
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            const adminModalEl = document.getElementById('adminDeleteCommentModal');
            const adminModal = bootstrap.Modal.getInstance(adminModalEl);
            adminModal.hide();
            showSuccessModal(data.message);
        } else {
            alert("Lỗi: " + data.message);
            this.disabled = false;
            this.innerText = 'Xóa và Gửi thông báo';
        }
    });
});

function showSuccessModal(message) {
    const successModalEl = document.getElementById('successModal');
    const successModal = new bootstrap.Modal(successModalEl);
    document.getElementById('successModalMessage').innerText = message;
    
    successModal.show();
    
    const reloadPage = () => {
        location.reload();
    };
    
    document.getElementById('successModalCloseBtn').addEventListener('click', reloadPage);
    successModalEl.addEventListener('hidden.bs.modal', reloadPage);
}
