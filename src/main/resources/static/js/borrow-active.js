function prepareReturnModal(button) {
    const transactionId = button.getAttribute('data-id');
    const bookName = button.getAttribute('data-name');
    
    document.getElementById('modalBookName').innerText = bookName;
    document.getElementById('returnForm').action = '/borrow/' + transactionId + '/return';
    
    // Đặt thời gian mặc định là hiện tại
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    document.getElementById('returnDateTime').value = now.toISOString().slice(0, 16);
}
