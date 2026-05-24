document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.btn-reject-request').forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const form = this.closest('form');
            if (typeof Swal !== 'undefined') {
                Swal.fire({
                    title: 'Từ chối yêu cầu mượn',
                    text: 'Vui lòng nhập lý do từ chối yêu cầu mượn truyện này:',
                    input: 'textarea',
                    inputPlaceholder: 'Nhập lý do tại đây...',
                    icon: 'warning',
                    showCancelButton: true,
                    confirmButtonColor: '#dc3545',
                    cancelButtonColor: '#6c757d',
                    confirmButtonText: 'Từ chối',
                    cancelButtonText: 'Hủy',
                    background: '#181818',
                    color: '#e0e0e0',
                    customClass: {
                        popup: 'border border-danger border-opacity-25 rounded-3',
                        input: 'bg-black text-light border-secondary text-light'
                    },
                    inputValidator: (value) => {
                        if (!value || !value.trim()) {
                            return 'Bạn cần nhập lý do từ chối!';
                        }
                    }
                }).then((result) => {
                    if (result.isConfirmed) {
                        const reasonInput = document.createElement('input');
                        reasonInput.type = 'hidden';
                        reasonInput.name = 'reason';
                        reasonInput.value = result.value;
                        form.appendChild(reasonInput);
                        form.submit();
                    }
                });
            } else {
                const reason = prompt('Nhập lý do từ chối yêu cầu mượn truyện này:');
                if (reason !== null && reason.trim() !== '') {
                    const reasonInput = document.createElement('input');
                    reasonInput.type = 'hidden';
                    reasonInput.name = 'reason';
                    reasonInput.value = reason;
                    form.appendChild(reasonInput);
                    form.submit();
                }
            }
        });
    });
});
