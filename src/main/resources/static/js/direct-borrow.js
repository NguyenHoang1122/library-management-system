$(document).ready(function() {
    $('.select2-user').select2({
        placeholder: "-- Tìm kiếm độc giả (Email, SĐT, Tên) --",
        allowClear: true,
        width: '100%'
    });

    $('.select2-book').select2({
        placeholder: "-- Tìm kiếm sách (Tiêu đề, ISBN) --",
        allowClear: true,
        width: '100%'
    });

    const BORROW_FEE = 30000;
    let cartItems = {};

    function formatCurrency(number) {
        return new Intl.NumberFormat('vi-VN').format(number) + ' đ';
    }

    function renderTable() {
        const tbody = $('#booksTbody');
        tbody.empty();
        let totalAmount = 0;

        if (Object.keys(cartItems).length === 0) {
            tbody.append('<tr><td colspan="6" class="text-center text-secondary">Chưa có truyện nào được chọn.</td></tr>');
        }

        for (let bookId in cartItems) {
            let item = cartItems[bookId];
            let itemTotal = (item.deposit + BORROW_FEE) * item.qty;
            totalAmount += itemTotal;

            let tr = $('<tr></tr>');
            tr.append(`<td>${item.title}
                <input type="hidden" name="bookIds" value="${bookId}">
                <input type="hidden" name="quantities" value="${item.qty}">
            </td>`);
            tr.append(`<td>${item.qty}</td>`);
            tr.append(`<td>${formatCurrency(item.deposit)}</td>`);
            tr.append(`<td>${formatCurrency(BORROW_FEE)}</td>`);
            tr.append(`<td class="text-info fw-bold">${formatCurrency(itemTotal)}</td>`);
            tr.append(`<td class="text-center"><button type="button" class="btn btn-sm btn-outline-danger btn-remove" data-id="${bookId}"><i class="bi bi-trash"></i></button></td>`);
            
            tbody.append(tr);
        }

        $('#totalAmount').text(formatCurrency(totalAmount));
    }

    renderTable(); // Initial empty render

    $('#btnAddBook').click(function() {
        let select = $('#bookSelect');
        let option = select.find('option:selected');
        if (!option.val()) {
            Swal.fire('Lỗi', 'Vui lòng chọn một truyện!', 'warning');
            return;
        }

        let bookId = option.val();
        let title = option.data('title');
        let maxQty = parseInt(option.data('qty'));
        let deposit = parseFloat(option.data('deposit'));
        let addQty = parseInt($('#bookQty').val());

        if (isNaN(addQty) || addQty <= 0) {
            Swal.fire('Lỗi', 'Số lượng không hợp lệ!', 'warning');
            return;
        }

        let currentQty = cartItems[bookId] ? cartItems[bookId].qty : 0;
        if (currentQty + addQty > maxQty) {
            Swal.fire('Lỗi', `Trong kho chỉ còn ${maxQty} cuốn.`, 'warning');
            return;
        }

        if (cartItems[bookId]) {
            cartItems[bookId].qty += addQty;
        } else {
            cartItems[bookId] = {
                title: title,
                deposit: deposit,
                qty: addQty
            };
        }

        $('#bookQty').val(1);
        select.val(null).trigger('change');
        renderTable();
    });

    $(document).on('click', '.btn-remove', function() {
        let bookId = $(this).data('id');
        delete cartItems[bookId];
        renderTable();
    });

    $('#btnResetForm').click(function() {
        $('#userId').val(null).trigger('change');
        $('#bookSelect').val(null).trigger('change');
        $('#bookQty').val(1);
        cartItems = {};
        renderTable();
    });

    $('#btnSubmitForm').click(function() {
        let userId = $('#userId').val();
        if (!userId) {
            Swal.fire('Lỗi', 'Vui lòng chọn độc giả!', 'warning');
            return;
        }

        if (Object.keys(cartItems).length === 0) {
            Swal.fire('Lỗi', 'Vui lòng thêm ít nhất 1 truyện vào đơn!', 'warning');
            return;
        }

        Swal.fire({
            title: 'Xác nhận tạo đơn',
            text: 'Hệ thống sẽ trừ trực tiếp tiền cọc và phí thuê từ Ví điện tử của độc giả. Bạn có chắc chắn?',
            icon: 'question',
            showCancelButton: true,
            confirmButtonText: 'Tạo đơn ngay',
            cancelButtonText: 'Hủy',
            confirmButtonColor: '#0d6efd',
            background: '#181818',
            color: '#e0e0e0'
        }).then((result) => {
            if (result.isConfirmed) {
                $('#directBorrowForm').submit();
            }
        });
    });
});
