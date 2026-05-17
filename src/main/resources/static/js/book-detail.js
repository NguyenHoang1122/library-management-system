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
                alert(data.message);
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
