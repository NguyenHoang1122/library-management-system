document.querySelectorAll('.btn-edit-book').forEach(button => {
    button.addEventListener('click', function() {
        const id = this.getAttribute('data-id');
        const title = this.getAttribute('data-title');
        const isbn = this.getAttribute('data-isbn');
        const description = this.getAttribute('data-description');
        const publishyear = this.getAttribute('data-publishyear');
        const quantity = this.getAttribute('data-quantity');
        const author = this.getAttribute('data-author');
        const importprice = this.getAttribute('data-importprice');
        const depositprice = this.getAttribute('data-depositprice');
        const categoriesStr = this.getAttribute('data-categories') || '';
        const cleanStr = categoriesStr.replace(/[\[\]\s]/g, '');
        const categoryIds = cleanStr ? cleanStr.split(',') : [];

        // Set form action
        document.getElementById('editBookForm').action = '/books/update/' + id;

        // Set inputs
        document.getElementById('editBookCurrentId').value = id;
        document.getElementById('editBookTitle').value = title;
        document.getElementById('editBookIsbn').value = isbn;
        document.getElementById('editBookDescription').value = description || '';
        document.getElementById('editBookPublishYear').value = publishyear || '';
        document.getElementById('editBookQuantity').value = quantity;
        document.getElementById('editBookAuthor').value = author || '';
        document.getElementById('editBookImportPrice').value = importprice || '';
        document.getElementById('editBookDepositPrice').value = depositprice || '';
        
        // Reset validation state
        document.getElementById('editBookIsbn').classList.remove('is-invalid');

        // Set categories checkboxes
        document.querySelectorAll('.edit-category-checkbox').forEach(checkbox => {
            checkbox.checked = categoryIds.includes(checkbox.value);
        });

        // Open Modal
        const editModal = new bootstrap.Modal(document.getElementById('editBookModal'));
        editModal.show();
    });
});

// Reset Add form validation state when opened
const addBookModalEl = document.getElementById('addBookModal');
if (addBookModalEl) {
    addBookModalEl.addEventListener('show.bs.modal', function () {
    const form = document.getElementById('addBookForm');
    if (form) {
        form.reset();
        // Clear is-invalid styling
        form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    }
    });
}

// Auto-open modals on validation failures
window.addEventListener('DOMContentLoaded', () => {
    const showAddModal = document.getElementById('showAddModalFlag') !== null;
    const showEditModalFlag = document.getElementById('showEditModalFlag');
    
    if (showAddModal) {
        const addModal = new bootstrap.Modal(document.getElementById('addBookModal'));
        addModal.show();
    } else if (showEditModalFlag !== null) {
        const editBookId = showEditModalFlag.getAttribute('data-id');
        document.getElementById('editBookForm').action = '/books/update/' + editBookId;
        document.getElementById('editBookCurrentId').value = editBookId;
        
        const editModal = new bootstrap.Modal(document.getElementById('editBookModal'));
        editModal.show();
    }
});

// SweetAlert2 delete confirmation for stories
document.querySelectorAll('.btn-delete-book').forEach(button => {
    button.addEventListener('click', function(e) {
        e.preventDefault();
        const deleteUrl = this.getAttribute('href');
        
        if (typeof Swal !== 'undefined') {
            Swal.fire({
                title: 'Xác nhận xóa?',
                text: "Bạn có chắc chắn muốn xóa cuốn truyện này không? Hành động này không thể hoàn tác!",
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#dc3545',
                cancelButtonColor: '#6c757d',
                confirmButtonText: 'Xóa ngay',
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
                    window.location.href = deleteUrl;
                }
            });
        } else {
            if (confirm('Bạn có chắc chắn muốn xóa cuốn truyện này không?')) {
                window.location.href = deleteUrl;
            }
        }
    });
});

