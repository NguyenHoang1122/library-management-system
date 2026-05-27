document.querySelectorAll('.btn-delete-word').forEach(button => {
    button.addEventListener('click', function(e) {
        e.preventDefault();
        const href = this.getAttribute('href');
        
        if (typeof Swal !== 'undefined') {
            Swal.fire({
                title: 'Xác nhận xóa?',
                text: "Từ nhạy cảm này sẽ bị xóa khỏi danh sách lọc !",
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
                    window.location.href = href;
                }
            });
        } else {
            if (confirm('Bạn có chắc muốn xóa từ này không?')) {
                window.location.href = href;
            }
        }
    });
});

// AI Settings and Playground Interactions
const toggleApiKeyBtn = document.getElementById('toggleApiKeyBtn');
const aiApiKeyInput = document.getElementById('aiApiKeyInput');
if (toggleApiKeyBtn && aiApiKeyInput) {
    toggleApiKeyBtn.addEventListener('click', function() {
        const type = aiApiKeyInput.getAttribute('type') === 'password' ? 'text' : 'password';
        aiApiKeyInput.setAttribute('type', type);
        const icon = this.querySelector('i');
        if (icon) {
            icon.className = type === 'password' ? 'bi bi-eye' : 'bi bi-eye-slash';
        }
    });
}

const btnRunAiTest = document.getElementById('btnRunAiTest');
const aiTestTextInput = document.getElementById('aiTestTextInput');
const aiTestResult = document.getElementById('aiTestResult');
const aiResultBadge = document.getElementById('aiResultBadge');
const aiResultWords = document.getElementById('aiResultWords');
const aiResultCensored = document.getElementById('aiResultCensored');
const aiResultReason = document.getElementById('aiResultReason');

if (btnRunAiTest && aiTestTextInput) {
    btnRunAiTest.addEventListener('click', function() {
        const text = aiTestTextInput.value.trim();
        if (!text) {
            if (typeof Swal !== 'undefined') {
                Swal.fire({
                    icon: 'warning',
                    title: 'Vui lòng nhập văn bản!',
                    text: 'Hãy điền nội dung bình luận cần chạy thử nghiệm.',
                    background: '#181818',
                    color: '#e0e0e0',
                    confirmButtonText: 'Đóng',
                    customClass: {
                        popup: 'border border-warning border-opacity-25 rounded-3',
                        confirmButton: 'btn btn-warning px-4 py-2 rounded-pill'
                    },
                    buttonsStyling: false
                });
            } else {
                alert('Vui lòng nhập văn bản cần chạy thử nghiệm!');
            }
            return;
        }

        // Set Loading State
        btnRunAiTest.disabled = true;
        btnRunAiTest.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> ĐANG PHÂN TÍCH...';
        aiTestResult.classList.add('d-none');

        // Fetch CSRF Token details
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

        const formData = new FormData();
        formData.append('text', text);

        const headers = {};
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        fetch('/admin/sensitive-words/ai-test', {
            method: 'POST',
            headers: headers,
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            btnRunAiTest.disabled = false;
            btnRunAiTest.innerHTML = '<i class="bi bi-lightning-charge-fill me-2"></i> THỬ NGHIỆM AI';

            if (data.success) {
                aiTestResult.classList.remove('d-none');
                
                if (data.sensitive) {
                    aiResultBadge.className = 'badge px-3 py-1.5 fs-6 rounded-pill bg-danger text-light';
                    aiResultBadge.innerText = 'NHẠY CẢM';
                    
                    const words = data.sensitiveWordsFound || [];
                    aiResultWords.className = 'fw-bold text-danger';
                    aiResultWords.innerText = words.length > 0 ? words.join(', ') : 'Không xác định cụ thể';
                    
                    aiResultCensored.innerText = data.censoredText || '';
                    aiResultReason.innerText = data.reason || 'Chứa ngôn từ không phù hợp';
                } else {
                    aiResultBadge.className = 'badge px-3 py-1.5 fs-6 rounded-pill bg-success text-light';
                    aiResultBadge.innerText = 'AN TOÀN';
                    
                    aiResultWords.className = 'fw-bold text-secondary';
                    aiResultWords.innerText = 'Không phát hiện';
                    
                    aiResultCensored.innerText = data.censoredText || text;
                    aiResultReason.innerText = 'Bình luận phù hợp tiêu chuẩn cộng đồng';
                }
            } else {
                if (typeof Swal !== 'undefined') {
                    Swal.fire({
                        icon: 'error',
                        title: 'Lỗi thử nghiệm AI!',
                        text: data.error || 'Có lỗi xảy ra trong quá trình xử lý.',
                        background: '#181818',
                        color: '#e0e0e0',
                        confirmButtonText: 'Đóng',
                        customClass: {
                            popup: 'border border-danger border-opacity-25 rounded-3',
                            confirmButton: 'btn btn-danger px-4 py-2 rounded-pill'
                        },
                        buttonsStyling: false
                    });
                } else {
                    alert('Lỗi: ' + (data.error || 'Có lỗi xảy ra trong quá trình xử lý.'));
                }
            }
        })
        .catch(err => {
            btnRunAiTest.disabled = false;
            btnRunAiTest.innerHTML = '<i class="bi bi-lightning-charge-fill me-2"></i> THỬ NGHIỆM AI';
            console.error(err);
            
            if (typeof Swal !== 'undefined') {
                Swal.fire({
                    icon: 'error',
                    title: 'Lỗi kết nối!',
                    text: 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại.',
                    background: '#181818',
                    color: '#e0e0e0',
                    confirmButtonText: 'Đóng',
                    customClass: {
                        popup: 'border border-danger border-opacity-25 rounded-3',
                        confirmButton: 'btn btn-danger px-4 py-2 rounded-pill'
                    },
                    buttonsStyling: false
                });
            } else {
                alert('Lỗi kết nối máy chủ!');
            }
        });
    });
}

