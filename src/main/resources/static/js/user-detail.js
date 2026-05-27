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

document.addEventListener("DOMContentLoaded", function() {
    const depositForm = document.getElementById('depositForm');
    if (depositForm) {
        depositForm.addEventListener('submit', function(e) {
            e.preventDefault();
            const amount = document.getElementById('amount').value;
            if (!amount || amount < 10000) {
                Swal.fire({
                    icon: 'warning',
                    title: 'Lỗi',
                    html: 'Số tiền phải lớn hơn 10.000đ',
                    confirmButtonColor: '#f39c12',
                    background: '#181818',
                    color: '#e0e0e0',
                    customClass: {
                        popup: 'border border-warning border-opacity-25 rounded-3'
                    }
                });
                return;
            }

            const csrfMeta = document.querySelector('meta[name="_csrf"]');
            const csrfToken = csrfMeta ? csrfMeta.getAttribute('content') : '';
            const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
            const csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.getAttribute('content') : '';

            const formData = new URLSearchParams();
            formData.append('amount', amount);

            const btn = document.getElementById('btnSubmitDeposit');
            const originalText = btn.innerHTML;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang xử lý...';
            btn.disabled = true;

            const headers = {
                'Content-Type': 'application/x-www-form-urlencoded'
            };
            if (csrfHeader && csrfToken) {
                headers[csrfHeader] = csrfToken;
            }

            fetch('/user/deposit', {
                method: 'POST',
                headers: headers,
                body: formData.toString()
            })
                .then(res => res.json())
                .then(data => {
                    if (data.success) {
                        if (data.redirectUrl) {
                            window.location.href = data.redirectUrl;
                        } else {
                            Swal.fire({
                                icon: 'success',
                                title: 'Thành công',
                                html: 'Đã nạp tiền thành công!',
                                confirmButtonColor: '#00b074',
                                background: '#181818',
                                color: '#e0e0e0',
                                customClass: {
                                    popup: 'border border-success border-opacity-25 rounded-3'
                                }
                            }).then(() => {
                                location.reload();
                            });
                        }
                    } else {
                        Swal.fire({
                            icon: 'error',
                            title: 'Lỗi',
                            html: data.message || 'Nạp tiền thất bại',
                            confirmButtonColor: '#d33',
                            background: '#181818',
                            color: '#e0e0e0',
                            customClass: {
                                popup: 'border border-danger border-opacity-25 rounded-3'
                            }
                        });
                        btn.innerHTML = originalText;
                        btn.disabled = false;
                    }
                })
                .catch(err => {
                    Swal.fire({
                        icon: 'error',
                        title: 'Lỗi',
                        html: 'Có lỗi kết nối',
                        confirmButtonColor: '#d33',
                        background: '#181818',
                        color: '#e0e0e0',
                        customClass: {
                            popup: 'border border-danger border-opacity-25 rounded-3'
                        }
                    });
                    btn.innerHTML = originalText;
                    btn.disabled = false;
                });
        });
    }

    const withdrawForm = document.getElementById('withdrawForm');
    if (withdrawForm) {
        withdrawForm.addEventListener('submit', function(e) {
            e.preventDefault();
            const amount = document.getElementById('withdrawAmount').value;
            if (!amount || amount < 10000) {
                Swal.fire({
                    icon: 'warning',
                    title: 'Lỗi',
                    html: 'Số tiền rút phải lớn hơn 10.000đ',
                    confirmButtonColor: '#f39c12',
                    background: '#181818',
                    color: '#e0e0e0',
                    customClass: {
                        popup: 'border border-warning border-opacity-25 rounded-3'
                    }
                });
                return;
            }

            const csrfMeta = document.querySelector('meta[name="_csrf"]');
            const csrfToken = csrfMeta ? csrfMeta.getAttribute('content') : '';
            const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
            const csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.getAttribute('content') : '';

            const formData = new URLSearchParams();
            formData.append('amount', amount);

            const btn = document.getElementById('btnSubmitWithdraw');
            const originalText = btn.innerHTML;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang xử lý...';
            btn.disabled = true;

            const headers = {
                'Content-Type': 'application/x-www-form-urlencoded'
            };
            if (csrfHeader && csrfToken) {
                headers[csrfHeader] = csrfToken;
            }

            fetch('/user/withdraw', {
                method: 'POST',
                headers: headers,
                body: formData.toString()
            })
                .then(res => res.json())
                .then(data => {
                    if (data.success) {
                        Swal.fire({
                            icon: 'success',
                            title: 'Thành công',
                            html: 'Đã rút tiền thành công!',
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
                        Swal.fire({
                            icon: 'error',
                            title: 'Lỗi',
                            html: data.message || 'Rút tiền thất bại',
                            confirmButtonColor: '#d33',
                            background: '#181818',
                            color: '#e0e0e0',
                            customClass: {
                                popup: 'border border-danger border-opacity-25 rounded-3'
                            }
                        });
                        btn.innerHTML = originalText;
                        btn.disabled = false;
                    }
                })
                .catch(err => {
                    Swal.fire({
                        icon: 'error',
                        title: 'Lỗi',
                        html: 'Có lỗi kết nối',
                        confirmButtonColor: '#d33',
                        background: '#181818',
                        color: '#e0e0e0',
                        customClass: {
                            popup: 'border border-danger border-opacity-25 rounded-3'
                        }
                    });
                    btn.innerHTML = originalText;
                    btn.disabled = false;
                });
        });
    }

    // Logic Address Selects
    const provinceSelect = document.getElementById("province");
    const districtSelect = document.getElementById("district");
    const wardSelect = document.getElementById("ward");
    const specificAddress = document.getElementById("specificAddress");
    const finalAddress = document.getElementById("finalAddress");
    const formProfile = finalAddress ? finalAddress.closest("form") : null;

    let savedProvince = "";
    let savedDistrict = "";
    let savedWard = "";
    let savedSpecific = "";

    if (finalAddress && finalAddress.value) {
        let parts = finalAddress.value.split(",").map(s => s.trim());
        if (parts.length >= 4) {
            savedProvince = parts.pop();
            savedDistrict = parts.pop();
            savedWard = parts.pop();
            savedSpecific = parts.join(", ");
        } else {
            savedSpecific = finalAddress.value;
        }
        if (specificAddress) specificAddress.value = savedSpecific;
    }

    if (provinceSelect) {
        // Fetch Provinces
        fetch('https://provinces.open-api.vn/api/p/')
            .then(response => response.json())
            .then(data => {
                if (data) {
                    let provinces = data;
                    provinces.forEach(p => {
                        let option = new Option(p.name, p.code);
                        provinceSelect.add(option);
                    });
                    if (savedProvince) {
                        for (let i = 0; i < provinceSelect.options.length; i++) {
                            if (provinceSelect.options[i].text === savedProvince) {
                                provinceSelect.selectedIndex = i;
                                provinceSelect.dispatchEvent(new Event("change"));
                                break;
                            }
                        }
                    }
                }
            });

        // Fetch Districts on Province Change
        provinceSelect.addEventListener("change", function() {
            if (districtSelect) districtSelect.length = 1;
            if (wardSelect) wardSelect.length = 1;
            const provinceId = this.value;
            if (provinceId && districtSelect) {
                fetch(`https://provinces.open-api.vn/api/p/${provinceId}?depth=2`)
                    .then(response => response.json())
                    .then(data => {
                        if (data && data.districts) {
                            data.districts.forEach(d => {
                                let option = new Option(d.name, d.code);
                                districtSelect.add(option);
                            });
                            if (savedDistrict) {
                                for (let i = 0; i < districtSelect.options.length; i++) {
                                    if (districtSelect.options[i].text === savedDistrict) {
                                        districtSelect.selectedIndex = i;
                                        districtSelect.dispatchEvent(new Event("change"));
                                        break;
                                    }
                                }
                                savedDistrict = ""; // clear after selection
                            }
                        }
                    });
            }
        });

        // Fetch Wards on District Change
        if (districtSelect) {
            districtSelect.addEventListener("change", function() {
                if (wardSelect) wardSelect.length = 1;
                const districtId = this.value;
                if (districtId && wardSelect) {
                    fetch(`https://provinces.open-api.vn/api/d/${districtId}?depth=2`)
                        .then(response => response.json())
                        .then(data => {
                            if (data && data.wards) {
                                data.wards.forEach(w => {
                                    let option = new Option(w.name, w.code);
                                    wardSelect.add(option);
                                });
                                if (savedWard) {
                                    for (let i = 0; i < wardSelect.options.length; i++) {
                                        if (wardSelect.options[i].text === savedWard) {
                                            wardSelect.selectedIndex = i;
                                            break;
                                        }
                                    }
                                    savedWard = ""; // clear after selection
                                }
                            }
                        });
                }
            });
        }
    }

    if (formProfile) {
        formProfile.addEventListener("submit", function(e) {
            let hasError = false;

            const phoneInput = document.getElementById("phoneNumberInput");
            const phoneErrorMsg = document.getElementById("phoneErrorMsg");
            
            if (phoneInput && (!phoneInput.value.trim() || !phoneInput.value.match(/^[0-9]{10,11}$/))) {
                if (phoneErrorMsg) phoneErrorMsg.style.display = "block";
                hasError = true;
            } else if (phoneErrorMsg) {
                phoneErrorMsg.style.display = "none";
            }

            const addressErrorMsg = document.getElementById("addressErrorMsg");
            let specific = specificAddress ? specificAddress.value.trim() : "";
            
            if (provinceSelect && districtSelect && wardSelect && specificAddress) {
                if (!provinceSelect.value || !districtSelect.value || !wardSelect.value || !specific) {
                    if (addressErrorMsg) addressErrorMsg.style.display = "block";
                    hasError = true;
                } else {
                    if (addressErrorMsg) addressErrorMsg.style.display = "none";
                }
            }

            if (hasError) {
                e.preventDefault();
                return;
            }

            // Combine address
            if (provinceSelect && districtSelect && wardSelect && specificAddress && finalAddress) {
                let provinceText = provinceSelect.options[provinceSelect.selectedIndex].text;
                let districtText = districtSelect.options[districtSelect.selectedIndex].text;
                let wardText = wardSelect.options[wardSelect.selectedIndex].text;
                finalAddress.value = `${specific}, ${wardText}, ${districtText}, ${provinceText}`;
            }
        });
    }
});
