package com.librarymanagementsystem.service.impl;

import com.librarymanagementsystem.model.borrow.status.UserStatus;
import com.librarymanagementsystem.model.user.Role;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.model.user.dto.UserDTO;
import com.librarymanagementsystem.model.user.status.RoleStatus;
import com.librarymanagementsystem.repository.RoleRepository;
import com.librarymanagementsystem.repository.UserRepository;
import com.librarymanagementsystem.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${file.upload-dir}")
    private String upload;

    @Override
    public User register(UserDTO userDTO) {
        if (userRepository.existsByUserName(userDTO.getUserName()) || userRepository.existsByEmail(userDTO.getEmail())) {
            throw new RuntimeException("Tên đăng nhập hoặc email đã tồn tại");
        }
        User user = new User();
        user.setFullName(userDTO.getFullName());
        user.setUserName(userDTO.getUserName());
        user.setEmail(userDTO.getEmail());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setAddress(userDTO.getAddress());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        Role userRole = roleRepository.findByRoleName(RoleStatus.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));
        user.setRole(userRole);

        user.setUserStatus(UserStatus.ACTIVE);

        user.setCreateDate(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public User login(String userName, String password) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new RuntimeException("Tên đăng nhập không tồn tại"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Mật khẩu không đúng");
        }
        return user;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> findByUserName(String name) {
        return userRepository.findByUserName(name);
    }


    @Override
    public User updateProfile(Long userId, UserDTO userDTO) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // Cập nhật thông tin
        user.setFullName(userDTO.getFullName());
        user.setUserName(userDTO.getUserName());
        user.setEmail(userDTO.getEmail());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setAddress(userDTO.getAddress());
        user.setUpdateDate(LocalDateTime.now());

        // Upload ảnh đại diện nếu có
        if (userDTO.getImageFile() != null && !userDTO.getImageFile().isEmpty()) {
            // Xóa ảnh cũ nếu tồn tại
            if (user.getImage() != null && !user.getImage().isEmpty()) {
                try {
                    Path oldPath = Paths.get(upload + user.getImage().substring(user.getImage().lastIndexOf("/") + 1));
                    Files.deleteIfExists(oldPath);
                } catch (IOException e) {
                    throw new RuntimeException("Lỗi xóa ảnh cũ: " + e.getMessage());
                }
            }
            // Upload ảnh mới
            String fileName = UUID.randomUUID().toString() + "_" + userDTO.getImageFile().getOriginalFilename();
            Path path = Paths.get(upload + fileName);
            try {
                Files.createDirectories(path.getParent());
                Files.write(path, userDTO.getImageFile().getBytes());
                user.setImage("/uploads/" + fileName);
            } catch (IOException e) {
                throw new RuntimeException("Lỗi upload ảnh đại diện: " + e.getMessage());
            }
        }

        return userRepository.save(user);
    }
}
