document.querySelectorAll('.btn-edit-book').forEach(button => {
    button.addEventListener('click', function() {
        const id = this.getAttribute('data-id');
        const title = this.getAttribute('data-title');
        const isbn = this.getAttribute('data-isbn');
        const description = this.getAttribute('data-description');
        const publishyear = this.getAttribute('data-publishyear');
        const quantity = this.getAttribute('data-quantity');
        const author = this.getAttribute('data-author');
        const categoriesStr = this.getAttribute('data-categories') || '';
        const cleanStr = categoriesStr.replace(/[\[\]\s]/g, '');
        const categoryIds = cleanStr ? cleanStr.split(',') : [];

        // Set form action
        document.getElementById('editBookForm').action = '/books/update/' + id;

        // Set inputs
        document.getElementById('editBookTitle').value = title;
        document.getElementById('editBookIsbn').value = isbn;
        document.getElementById('editBookDescription').value = description || '';
        document.getElementById('editBookPublishYear').value = publishyear || '';
        document.getElementById('editBookQuantity').value = quantity;
        document.getElementById('editBookAuthor').value = author || '';

        // Set categories checkboxes
        document.querySelectorAll('.edit-category-checkbox').forEach(checkbox => {
            checkbox.checked = categoryIds.includes(checkbox.value);
        });

        // Open Modal
        const editModal = new bootstrap.Modal(document.getElementById('editBookModal'));
        editModal.show();
    });
});
