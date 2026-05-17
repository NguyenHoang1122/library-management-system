package com.librarymanagementsystem.service;


import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.model.user.dto.UserDTO;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User register(UserDTO userDTO);
    User login(String userName, String password);
    List<User> getAllUsers();
    Optional<User> findByUserName(String name);
    Optional<User> findById(Long id);
    User updateProfile(Long userId, UserDTO userDTO);

    List<User> getAllActiveUsers();
    List<User> getAllDeletedUsers();
    void softDeleteUser(Long userId);
    void restoreUser(Long userId);
    void permanentlyDeleteUser(Long userId);
    void changeUserRole(Long userId, String roleName);
    void permanentlyDeleteOldUsers();
}
