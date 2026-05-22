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
                }).then(() => {
                    // Update cart badge logic if needed
                    location.reload();
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

// Submit review form via AJAX
const reviewForm = document.getElementById('reviewForm');
if (reviewForm) {
    reviewForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const bookId = reviewForm.getAttribute('data-book-id');
        const rating = selectedRatingInput.value;
        const comment = document.getElementById('reviewComment').value;
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

        const submitBtn = document.getElementById('submitReviewBtn');
        submitBtn.disabled = true;
        const originalText = submitBtn.innerText;
        submitBtn.innerText = 'Đang gửi...';

        fetch(`/books/${bookId}/review`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                [csrfHeader]: csrfToken
            },
            body: `rating=${rating}&comment=${encodeURIComponent(comment)}`
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
