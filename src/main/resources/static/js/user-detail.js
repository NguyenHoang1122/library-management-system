// Preview hình ảnh khi chọn file
document.getElementById('imageFile').addEventListener('change', function (event) {
    const file = event.target.files[0];
    if (file) {
        // Kiểm tra dung lượng (10MB)
        if (file.size > 10 * 1024 * 1024) {
            alert("Dung lượng file quá lớn! Vui lòng chọn file dưới 10MB.");
            this.value = "";
            return;
        }
        const reader = new FileReader();
        reader.onload = function (e) {
            document.getElementById('previewImage').src = e.target.result;
            document.getElementById('fileName').textContent = '✓ ' + file.name;
        };
        reader.readAsDataURL(file);
    }
});
