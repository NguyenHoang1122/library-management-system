package com.librarymanagementsystem.service.user;


import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.model.user.dto.UserDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import java.math.BigDecimal;

public interface UserService {

    User register(UserDTO userDTO);

    User login(String userName, String password);

    List<User> getAllUsers();

    Optional<User> findByUserName(String name);

    Optional<User> findById(Long id);

    User updateProfile(Long userId, UserDTO userDTO);

    List<User> getAllActiveUsers();

    org.springframework.data.domain.Page<User> getActiveUsers(String query, Pageable pageable);

    List<User> getAllDeletedUsers();

    org.springframework.data.domain.Page<User> getDeletedUsers(String query, Pageable pageable);

    // Xóa mềm
    void softDeleteUser(Long userId);

    // Khôi phục tài khoản
    void restoreUser(Long userId);

    // Xóa vĩnh viễn
    void permanentlyDeleteUser(Long userId);

    // Thay đổi vai trò
    void changeUserRole(Long userId, String roleName);

    // Tự động xóa vĩnh viễn các user
    void permanentlyDeleteOldUsers();
    //nap tiền
    BigDecimal deposit(Long userId, BigDecimal amount);
    //rut tiền
    BigDecimal withdraw(Long userId, BigDecimal amount);
}
