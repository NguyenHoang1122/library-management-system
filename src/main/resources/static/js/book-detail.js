const wishlistBtn = document.getElementById('wishlistBtn');
const borrowBtn = document.getElementById('borrowBtn');

if (wishlistBtn) {
    const bookId = wishlistBtn.getAttribute('data-book-id');
    wishlistBtn.addEventListener('click', () => addToWishlist(bookId));
    checkWishlistStatus(bookId);
}

if (borrowBtn) {
    const bookId = borrowBtn.getAttribute('data-book-id');
    const borrowModal = new bootstrap.Modal(document.getElementById('borrowModal'));
    const confirmBorrowBtn = document.getElementById('confirmBorrowBtn');
    const borrowNote = document.getElementById('borrowNote');

    borrowBtn.addEventListener('click', () => {
        borrowModal.show();
    });

    confirmBorrowBtn.addEventListener('click', () => {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        const note = borrowNote.value; 
        
        confirmBorrowBtn.disabled = true;
        confirmBorrowBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Đang gửi...';

        fetch(`/borrow/quick-request/${bookId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                [csrfHeader]: csrfToken
            },
            body: `note=${encodeURIComponent(note)}`
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                borrowModal.hide();
                showSuccessModal("Đã gửi yêu cầu mượn thành công");
                borrowBtn.parentElement.innerHTML = `
                    <button class="btn btn-secondary rounded-pill px-4 disabled">
                        <i class="fas fa-check-circle"></i> Đang chờ duyệt
                    </button>
                `;
                const stockValue = document.querySelector('.info-item:last-child .value');
                if (stockValue) {
                    const currentText = stockValue.innerText;
                    const match = currentText.match(/(\d+)/);
                    if (match) {
                        const newQty = parseInt(match[1]) - 1;
                        stockValue.innerText = newQty > 0 ? `${newQty} cuốn` : "Hết hàng";
                    } else if (currentText.includes("1")) {
                         stockValue.innerText = "Hết hàng";
                    }
                }
            } else {
                alert("Lỗi: " + data.message);
                confirmBorrowBtn.disabled = false;
                confirmBorrowBtn.innerText = 'Xác nhận mượn';
            }
        })
        .catch(err => {
            console.error(err);
            alert("Đã có lỗi xảy ra khi gửi yêu cầu.");
            confirmBorrowBtn.disabled = false;
            confirmBorrowBtn.innerText = 'Xác nhận mượn';
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
