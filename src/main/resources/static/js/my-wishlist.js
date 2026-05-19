let currentBorrowBookId = null;

function prepareBorrowModal(button) {
    currentBorrowBookId = button.getAttribute('data-id');
    const bookName = button.getAttribute('data-name');
    document.getElementById('modalBorrowBookName').innerText = bookName;
    document.getElementById('borrowNote').value = '';
}

document.addEventListener("DOMContentLoaded", () => {
    const confirmBorrowBtn = document.getElementById('confirmBorrowBtn');
    if (confirmBorrowBtn) {
        confirmBorrowBtn.addEventListener('click', () => {
            if (!currentBorrowBookId) return;
            
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
            const note = document.getElementById('borrowNote').value;
            
            confirmBorrowBtn.disabled = true;
            confirmBorrowBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Đang gửi...';

            fetch(`/borrow/quick-request/${currentBorrowBookId}`, {
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
                    // Hide the modal
                    const modalEl = document.getElementById('borrowModal');
                    const modalInstance = bootstrap.Modal.getInstance(modalEl);
                    if (modalInstance) {
                        modalInstance.hide();
                    }

                    // Dynamically update the borrow button to 'Đang chờ duyệt'
                    const borrowBtn = document.querySelector(`.btn-borrow[data-id="${currentBorrowBookId}"]`);
                    if (borrowBtn) {
                        const newBtn = document.createElement('button');
                        newBtn.className = 'btn btn-secondary btn-action-sm disabled';
                        newBtn.style.flex = '2';
                        newBtn.innerText = 'Đang chờ duyệt';
                        borrowBtn.replaceWith(newBtn);
                    }

                    if (typeof Swal !== 'undefined') {
                        Swal.fire({
                            icon: 'success',
                            title: 'Thành công',
                            text: 'Đã gửi yêu cầu mượn thành công!',
                            confirmButtonColor: '#00b074',
                            background: '#181818',
                            color: '#e0e0e0',
                            customClass: {
                                popup: 'border border-success border-opacity-25 rounded-3'
                            }
                        }).then(() => {
                            location.reload();
                        });
                    } else {
                        alert("Đã gửi yêu cầu mượn thành công!");
                        location.reload();
                    }
                } else {
                    if (typeof Swal !== 'undefined') {
                        Swal.fire({
                            icon: 'error',
                            title: 'Thất bại',
                            text: data.message,
                            confirmButtonColor: '#dc3545',
                            background: '#181818',
                            color: '#e0e0e0',
                            customClass: {
                                popup: 'border border-danger border-opacity-25 rounded-3'
                            }
                        });
                    } else {
                        alert("Lỗi: " + data.message);
                    }
                    confirmBorrowBtn.disabled = false;
                    confirmBorrowBtn.innerText = 'Xác nhận mượn';
                }
            })
            .catch(err => {
                console.error(err);
                if (typeof Swal !== 'undefined') {
                    Swal.fire({
                        icon: 'error',
                        title: 'Lỗi',
                        text: 'Đã có lỗi xảy ra khi gửi yêu cầu.',
                        confirmButtonColor: '#dc3545',
                        background: '#181818',
                        color: '#e0e0e0'
                    });
                } else {
                    alert("Đã có lỗi xảy ra khi gửi yêu cầu.");
                }
                confirmBorrowBtn.disabled = false;
                confirmBorrowBtn.innerText = 'Xác nhận mượn';
            });
        });
    }
    
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
