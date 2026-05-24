const form = document.getElementById('registerForm');
const password = document.getElementById('password');
const confirmPassword = document.getElementById('confirmPassword');
const message = document.getElementById('matchMessage');

function validatePassword() {
    // Nếu ô xác nhận mật khẩu còn trống, chưa cần báo lỗi
    if (confirmPassword.value === "") {
        message.style.display = 'none';
        confirmPassword.setCustomValidity('');
        return true;
    }

    if (password.value !== confirmPassword.value) {
        message.style.display = 'block';
        confirmPassword.setCustomValidity("Passwords Don't Match");
        return false;
    } else {
        message.style.display = 'none';
        confirmPassword.setCustomValidity('');
        return true;
    }
}

if (confirmPassword && password) {
    confirmPassword.onkeyup = validatePassword;
    password.onkeyup = validatePassword;
}

if (form) {
    form.onsubmit = function(e) {
        if (!validatePassword()) {
            e.preventDefault();
            alert("Vui lòng kiểm tra lại mật khẩu xác nhận!");
        }
    };
}
