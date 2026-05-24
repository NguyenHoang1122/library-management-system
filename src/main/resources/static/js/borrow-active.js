function prepareReturnModal(button) {
    const transactionId = button.getAttribute('data-id');
    const bookName = button.getAttribute('data-name');
    const dueDateStr = button.getAttribute('data-due-date');
    
    document.getElementById('modalBookName').innerText = bookName;
    document.getElementById('returnForm').action = '/borrow/' + transactionId + '/return';
    
    const now = new Date();
    
    // Mặc định ngày là ngày hạn trả (dueDateStr) được truyền từ button
    let dateStr = "";
    if (dueDateStr) {
        const parts = dueDateStr.split('/');
        if (parts.length === 3) {
            // Định dạng từ dd/MM/yyyy sang yyyy-MM-dd
            dateStr = `${parts[2]}-${parts[1]}-${parts[0]}`;
        }
    }
    
    // Fallback sang ngày hiện tại nếu không lấy được hạn trả
    if (!dateStr) {
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        dateStr = `${year}-${month}-${day}`;
    }
    
    // Định dạng giờ HH và phút mm của thời điểm hiện tại
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    
    document.getElementById('friendlyReturnDate').value = dateStr;
    document.getElementById('friendlyReturnHour').value = hours;
    document.getElementById('friendlyReturnMinute').value = minutes;
    
    updateReturnDateTime();
}

function updateReturnDateTime() {
    const dateVal = document.getElementById('friendlyReturnDate').value;
    const hourVal = document.getElementById('friendlyReturnHour').value;
    const minVal = document.getElementById('friendlyReturnMinute').value;
    if (dateVal && hourVal && minVal) {
        document.getElementById('returnDateTime').value = `${dateVal}T${hourVal}:${minVal}`;
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const returnForm = document.getElementById('returnForm');
    if (returnForm) {
        document.getElementById('friendlyReturnDate').addEventListener('change', updateReturnDateTime);
        document.getElementById('friendlyReturnHour').addEventListener('change', updateReturnDateTime);
        document.getElementById('friendlyReturnMinute').addEventListener('change', updateReturnDateTime);
        
        returnForm.addEventListener('submit', (e) => {
            updateReturnDateTime();
        });
    }
});
