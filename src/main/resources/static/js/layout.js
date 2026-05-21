document.addEventListener('DOMContentLoaded', () => {
    // 1. Notification mark read click handler
    const markReadBtns = document.querySelectorAll('.mark-read-btn');
    markReadBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            
            const notificationId = btn.getAttribute('data-id');
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
            
            btn.disabled = true;
            
            fetch(`/my-notifications/${notificationId}/mark-read-ajax`, {
                method: 'POST',
                headers: {
                    [csrfHeader]: csrfToken
                }
            })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    // Update the item appearance to "read" state
                    const itemContainer = btn.closest('.dropdown-item');
                    if (itemContainer) {
                        itemContainer.classList.remove('bg-dark', 'bg-opacity-50');
                        
                        const titleSpan = itemContainer.querySelector('.notification-title');
                        if (titleSpan) {
                            titleSpan.classList.remove('text-light');
                            titleSpan.classList.add('text-secondary', 'font-weight-normal');
                        }
                    }
                    
                    // Replace the mark as read button with double checkmark
                    const parentDiv = btn.parentElement;
                    if (parentDiv) {
                        parentDiv.innerHTML = `
                            <span class="text-muted text-opacity-50" title="Đã đọc">
                                <i class="bi bi-check2-all fs-5"></i>
                            </span>
                        `;
                    }
                    
                    // Decrement the badge count
                    const badge = document.getElementById('notificationBadge');
                    if (badge) {
                        const currentCount = parseInt(badge.innerText) || 0;
                        const newCount = currentCount - 1;
                        if (newCount > 0) {
                            badge.innerText = newCount;
                        } else {
                            badge.remove();
                        }
                    }
                } else {
                    console.error('Failed to mark read:', data.message);
                    btn.disabled = false;
                }
            })
            .catch(err => {
                console.error(err);
                btn.disabled = false;
            });
        });
    });

    // 2. Mark all notifications as read click handler (AJAX)
    const markAllReadBtn = document.getElementById('markAllReadNotificationsBtn');
    if (markAllReadBtn) {
        markAllReadBtn.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
            
            markAllReadBtn.disabled = true;
            
            fetch('/my-notifications/mark-all-read-ajax', {
                method: 'POST',
                headers: {
                    [csrfHeader]: csrfToken
                }
            })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    if (typeof Swal !== 'undefined') {
                        Swal.fire({
                            icon: 'success',
                            title: 'Thành công',
                            html: data.message,
                            confirmButtonColor: '#00b074',
                            background: '#181818',
                            color: '#e0e0e0',
                            customClass: {
                                popup: 'border border-success border-opacity-25 rounded-3'
                            }
                        });
                    } else {
                        alert(data.message);
                    }
                    
                    // Update all items in the dropdown list to "read" state
                    const dropdownMenu = markAllReadBtn.closest('.notification-list');
                    if (dropdownMenu) {
                        const items = dropdownMenu.querySelectorAll('li');
                        items.forEach(li => {
                            // Skip header li and non-notification items
                            if (li.classList.contains('notification-header') || li.querySelector('.dropdown-item') == null) {
                                return;
                            }
                            
                            const itemDiv = li.querySelector('.dropdown-item');
                            if (itemDiv) {
                                // Remove unread styling
                                itemDiv.classList.remove('bg-dark', 'bg-opacity-50');
                                
                                // Update title class
                                const titleSpan = itemDiv.querySelector('.notification-title');
                                if (titleSpan) {
                                    titleSpan.classList.remove('text-light');
                                    titleSpan.classList.add('text-secondary');
                                }
                                
                                // Replace mark-read button with double checkmark icon
                                const markReadBtnDiv = itemDiv.querySelector('.ms-2.px-2');
                                if (markReadBtnDiv) {
                                    markReadBtnDiv.innerHTML = `
                                        <span class="text-muted text-opacity-50" title="Đã đọc">
                                            <i class="bi bi-check2-all fs-5"></i>
                                        </span>
                                    `;
                                }
                            }
                        });
                    }
                    
                    // Remove/hide badge if present
                    const badge = document.getElementById('notificationBadge');
                    if (badge) {
                        badge.remove();
                    }
                    
                    markAllReadBtn.disabled = false;
                } else {
                    if (typeof Swal !== 'undefined') {
                        Swal.fire({
                            icon: 'error',
                            title: 'Thất bại',
                            html: data.message,
                            confirmButtonColor: '#dc3545',
                            background: '#181818',
                            color: '#e0e0e0',
                            customClass: {
                                popup: 'border border-danger border-opacity-25 rounded-3'
                            }
                        });
                    } else {
                        alert(data.message);
                    }
                    markAllReadBtn.disabled = false;
                }
            })
            .catch(err => {
                console.error(err);
                markAllReadBtn.disabled = false;
            });
        });
    }

    // 3. Delete read notifications click handler (AJAX)
    const deleteReadBtn = document.getElementById('deleteReadNotificationsBtn');
    if (deleteReadBtn) {
        deleteReadBtn.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
            
            deleteReadBtn.disabled = true;
            
            fetch('/my-notifications/delete-read-ajax', {
                method: 'POST',
                headers: {
                    [csrfHeader]: csrfToken
                }
            })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    if (typeof Swal !== 'undefined') {
                        Swal.fire({
                            icon: 'success',
                            title: 'Thành công',
                            html: data.message,
                            confirmButtonColor: '#00b074',
                            background: '#181818',
                            color: '#e0e0e0',
                            customClass: {
                                popup: 'border border-success border-opacity-25 rounded-3'
                            }
                        });
                    } else {
                        alert(data.message);
                    }
                    
                    // Dynamically remove all read notification items from DOM dropdown
                    const dropdownMenu = deleteReadBtn.closest('.notification-list');
                    if (dropdownMenu) {
                        const items = dropdownMenu.querySelectorAll('li');
                        let remainingCount = 0;
                        items.forEach(li => {
                            // Skip header li and non-notification items
                            if (li.classList.contains('notification-header') || li.querySelector('.dropdown-item') == null) {
                                return;
                            }
                            
                            const itemDiv = li.querySelector('.dropdown-item');
                            if (itemDiv) {
                                // If it's read (does not have unread highlight bg-dark bg-opacity-50), remove it
                                const isUnread = itemDiv.classList.contains('bg-dark') && itemDiv.classList.contains('bg-opacity-50');
                                if (!isUnread) {
                                    li.remove();
                                } else {
                                    remainingCount++;
                                }
                            }
                        });
                        
                        // If no notifications left, show empty placeholder
                        if (remainingCount === 0) {
                            dropdownMenu.innerHTML = `
                                <li><a class="dropdown-item text-center py-3 text-secondary">Không có thông báo</a></li>
                            `;
                            // Also remove/hide badge if present
                            const badge = document.getElementById('notificationBadge');
                            if (badge) badge.remove();
                        }
                    }
                    deleteReadBtn.disabled = false;
                } else {
                    if (typeof Swal !== 'undefined') {
                        Swal.fire({
                            icon: 'info',
                            title: 'Thông báo',
                            html: data.message,
                            confirmButtonColor: '#00b074',
                            background: '#181818',
                            color: '#e0e0e0',
                            customClass: {
                                popup: 'border border-info border-opacity-25 rounded-3'
                            }
                        });
                    } else {
                        alert(data.message);
                    }
                    deleteReadBtn.disabled = false;
                }
            })
            .catch(err => {
                console.error(err);
                deleteReadBtn.disabled = false;
            });
        });
    }

    // 2. Global SweetAlert2 checking logic
    const flashEl = document.getElementById('flash-alerts');
    if (flashEl && typeof Swal !== 'undefined') {
        const message = flashEl.getAttribute('data-message');
        const error = flashEl.getAttribute('data-error');
        
        if (message) {
            Swal.fire({
                icon: 'success',
                title: 'Thành công',
                html: message,
                confirmButtonColor: '#00b074',
                background: '#181818',
                color: '#e0e0e0',
                customClass: {
                    popup: 'border border-success border-opacity-25 rounded-3'
                }
            });
        }
        
        if (error) {
            Swal.fire({
                icon: 'error',
                title: 'Thất bại',
                html: error,
                confirmButtonColor: '#dc3545',
                background: '#181818',
                color: '#e0e0e0',
                customClass: {
                    popup: 'border border-danger border-opacity-25 rounded-3'
                }
            });
        }
    }
});
