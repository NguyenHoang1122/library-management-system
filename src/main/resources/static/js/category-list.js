document.querySelectorAll('.btn-edit-category').forEach(button => {
    button.addEventListener('click', function() {
        const id = this.getAttribute('data-id');
        const name = this.getAttribute('data-name');
        
        // Cập nhật action của form và giá trị input
        document.getElementById('editCategoryForm').action = '/categories/update/' + id;
        document.getElementById('editCategoryName').value = name;
        
        // Hiển thị modal
        const editModal = new bootstrap.Modal(document.getElementById('editCategoryModal'));
        editModal.show();
    });
});
