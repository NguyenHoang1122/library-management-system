function increaseQty(transactionId, bookId, max) {
    const input = document.getElementById('qty-' + transactionId + '-' + bookId);
    let val = parseInt(input.value);
    if (val < max) {
        input.value = val + 1;
    }
}

function decreaseQty(transactionId, bookId) {
    const input = document.getElementById('qty-' + transactionId + '-' + bookId);
    let val = parseInt(input.value);
    if (val > 0) {
        input.value = val - 1;
    }
}

function openDirectReturnModal(transactionId, isOverdue, userId) {
    let selectedItemIds = [];
    
    // Find all qty inputs for this transaction
    const qtyInputs = document.querySelectorAll('[id^="qty-' + transactionId + '-"]');
    qtyInputs.forEach(input => {
        const qty = parseInt(input.value);
        if (qty > 0) {
            const parts = input.id.split('-');
            const bookId = parts[2];
            const unreturnedStr = document.getElementById('unreturned-' + transactionId + '-' + bookId).value;
            if (unreturnedStr) {
                const ids = unreturnedStr.split(',');
                for(let i=0; i<qty; i++) {
                    selectedItemIds.push(ids[i]);
                }
            }
        }
    });

    if (selectedItemIds.length === 0) {
        Swal.fire({
            icon: 'warning',
            title: 'Chưa chọn số lượng',
            text: 'Vui lòng chọn số lượng truyện để trả!',
            background: '#181818',
            color: '#e0e0e0'
        });
        return;
    }

    let htmlContent = isOverdue ? 
        '<p class="text-danger fw-bold"><i class="bi bi-exclamation-triangle-fill me-2"></i>Truyện đã quá hạn!</p><p>Bạn có chắc chắn muốn xác nhận trả ' + selectedItemIds.length + ' cuốn truyện này và ghi nhận phạt?</p>' :
        '<p>Bạn có chắc chắn muốn xác nhận độc giả đã trả trực tiếp ' + selectedItemIds.length + ' cuốn truyện này tại quầy?</p>';

    Swal.fire({
        title: 'Xác nhận trả trực tiếp',
        html: htmlContent,
        icon: isOverdue ? 'warning' : 'question',
        showCancelButton: true,
        confirmButtonColor: '#198754',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'XÁC NHẬN',
        cancelButtonText: 'Hủy',
        background: '#181818',
        color: '#e0e0e0'
    }).then((result) => {
        if (result.isConfirmed) {
            // Tạo một form ẩn để submit
            const form = document.createElement('form');
            form.method = 'post';
            form.action = '/librarian/active-borrows/return/' + transactionId;

            // Thêm CSRF token nếu có
            const csrfInput = document.querySelector('input[name="_csrf"]');
            if (csrfInput) {
                const hiddenCsrf = document.createElement('input');
                hiddenCsrf.type = 'hidden';
                hiddenCsrf.name = csrfInput.name;
                hiddenCsrf.value = csrfInput.value;
                form.appendChild(hiddenCsrf);
            }

            // Thêm userId
            const userIdInput = document.createElement('input');
            userIdInput.type = 'hidden';
            userIdInput.name = 'userId';
            userIdInput.value = userId;
            form.appendChild(userIdInput);

            // Thêm itemIds
            selectedItemIds.forEach(id => {
                const hidden = document.createElement('input');
                hidden.type = 'hidden';
                hidden.name = 'itemIds';
                hidden.value = id;
                form.appendChild(hidden);
            });

            document.body.appendChild(form);
            form.submit();
        }
    });
}
