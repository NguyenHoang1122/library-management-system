document.getElementById('saveRoleChanges').addEventListener('click', function () {
    const selects = document.querySelectorAll('.role-select');
    let hasChanges = false;
    selects.forEach(select => {
        const userId = select.getAttribute('data-user-id');
        const newRole = select.value;
        hasChanges = true;
        const form = document.createElement('form');
        form.method = 'post';
        form.action = `/user/${userId}/change-role`;
        const input = document.createElement('input');
        input.type = 'hidden'; input.name = 'roleName'; input.value = newRole;
        form.appendChild(input);
        document.body.appendChild(form);
        form.submit();
    });

    if (!hasChanges) alert('Không có thay đổi nào.');
});
