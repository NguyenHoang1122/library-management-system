package com.librarymanagementsystem.service.user.impl;

import com.librarymanagementsystem.model.borrow.status.UserStatus;
import com.librarymanagementsystem.model.user.Role;
import com.librarymanagementsystem.model.user.User;
import com.librarymanagementsystem.model.user.dto.UserDTO;
import com.librarymanagementsystem.model.user.status.RoleStatus;
import com.librarymanagementsystem.repository.user.RoleRepository;
import com.librarymanagementsystem.repository.user.UserRepository;
import com.librarymanagementsystem.service.user.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

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
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public User updateProfile(Long userId, UserDTO userDTO) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (userDTO.getPhoneNumber() == null || userDTO.getPhoneNumber().trim().isEmpty()) {
            throw new RuntimeException("Số điện thoại không được để trống");
        }
        if (!userDTO.getPhoneNumber().matches("^[0-9]{10,11}$")) {
            throw new RuntimeException("Số điện thoại phải là 10-11 chữ số");
        }
        if (userDTO.getAddress() == null || userDTO.getAddress().trim().isEmpty()) {
            throw new RuntimeException("Địa chỉ không được để trống");
        }

        // Cập nhật thông tin
        user.setFullName(userDTO.getFullName());
        user.setUserName(userDTO.getUserName());
        user.setEmail(userDTO.getEmail());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setAddress(userDTO.getAddress());
        user.setUpdateDate(LocalDateTime.now());

        // Upload ảnh đại diện nếu có
        if (userDTO.getImageFile() != null && !userDTO.getImageFile().isEmpty()) {
            // Xác thực file ảnh (MIME type và extension) bảo mật
            com.librarymanagementsystem.util.ImageUploadValidator.validateImage(userDTO.getImageFile());

            // Xóa ảnh cũ nếu tồn tại
            if (user.getImage() != null && !user.getImage().isEmpty()) {
                try {
                    String oldFileName = user.getImage().substring(user.getImage().lastIndexOf("/") + 1);
                    Path oldPath = Paths.get(upload, oldFileName);
                    Files.deleteIfExists(oldPath);
                } catch (IOException e) {
                    System.err.println("Cảnh báo: Không thể xóa ảnh cũ: " + e.getMessage());
                }
            }

            // Upload ảnh mới
            String fileName = UUID.randomUUID().toString() + "_" + userDTO.getImageFile().getOriginalFilename();
            Path uploadPath = Paths.get(upload);
            try {
                Files.createDirectories(uploadPath);
                Path filePath = uploadPath.resolve(fileName);
                Files.write(filePath, userDTO.getImageFile().getBytes());
                user.setImage("/uploads/" + fileName);
            } catch (IOException e) {
                throw new RuntimeException("Lỗi upload ảnh đại diện: " + e.getMessage());
            }
        }

        return userRepository.save(user);
    }

    // danh sách người dùng đang hoạt động
    @Override
    public List<User> getAllActiveUsers() {
        return userRepository.findAllActiveUsers();
    }

    @Override
    public Page<User> getActiveUsers(String query, Pageable pageable) {
        return userRepository.findActiveUsers(query, pageable);
    }

    // danh sách người bị xóa mềm
    @Override
    public List<User> getAllDeletedUsers() {
        return userRepository.findAllDeletedUsers();
    }

    @Override
    public Page<User> getDeletedUsers(String query, Pageable pageable) {
        return userRepository.findDeletedUsers(query, pageable);
    }

    // Xóa mềm tài khoản
    @Override
    public void softDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (user.getRole().getRoleName() == RoleStatus.ROLE_ADMIN) {
            throw new RuntimeException("Không thể xóa tài khoản Admin");
        }
        user.setUserStatus(UserStatus.BANNED);
        user.setDeleteAt(LocalDateTime.now());
        userRepository.save(user);
    }

    // Khôi phục tài khoản
    @Override
    public void restoreUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        user.setUserStatus(UserStatus.ACTIVE);
        user.setDeleteAt(null);
        userRepository.save(user);
    }

    // Xóa vĩnh viễn
    public void permanentlyDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        // Không cho phép xóa ADMIN
        if (user.getRole().getRoleName() == RoleStatus.ROLE_ADMIN) {
            throw new RuntimeException("Không thể xóa vĩnh viễn tài khoản Admin");
        }

        // Xóa ảnh đại diện nếu có
        if (user.getImage() != null && !user.getImage().isEmpty()) {
            try {
                String fileName = user.getImage().substring(user.getImage().lastIndexOf("/") + 1);
                Path path = Paths.get(upload, fileName);
                Files.deleteIfExists(path);
            } catch (IOException e) {
                System.err.println("Cảnh báo: Không thể xóa ảnh đại diện: " + e.getMessage());
            }
        }

        userRepository.hardDeleteById(userId);
    }

    // Thay đổi vai trò
    @Override
    public void changeUserRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (user.getRole().getRoleName() == RoleStatus.ROLE_ADMIN) {
            throw new RuntimeException("Không thể thay đổi role của tài khoản Admin");
        }

        RoleStatus roleStatus;
        try {
            roleStatus = RoleStatus.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Role không hợp lệ");
        }

        Role role = roleRepository.findByRoleName(roleStatus)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));

        user.setRole(role);
        userRepository.save(user);
    }

    // check hàng ngày để xóa tài khoản bị xóa mềm
    @Override
    @Scheduled(cron = "0 0 2 * * ?")
    public void permanentlyDeleteOldUsers() {
        LocalDateTime cutoffDate = LocalDateTime.now().minus(5, ChronoUnit.DAYS);
        List<User> usersToDelete = userRepository.findUsersToPermanentlyDelete(cutoffDate);

        for (User user : usersToDelete) {
            // Xóa ảnh đại diện
            if (user.getImage() != null && !user.getImage().isEmpty()) {
                try {
                    String fileName = user.getImage().substring(user.getImage().lastIndexOf("/") + 1);
                    Path path = Paths.get(upload, fileName);
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    System.err.println("Cảnh báo: Không thể xóa ảnh đại diện: " + e.getMessage());
                }
            }
        }

        userRepository.permanentlyDeleteOldUsers(cutoffDate);
    }

    @Override
    public BigDecimal deposit(Long userId, BigDecimal amount) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        BigDecimal oldBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = oldBalance.add(amount);
        user.setBalance(newBalance);
        userRepository.save(user);
        return newBalance;
    }

    @Override
    public BigDecimal withdraw(Long userId, BigDecimal amount) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        if (currentBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Số dư không đủ để rút");
        }
        BigDecimal newBalance = currentBalance.subtract(amount);
        user.setBalance(newBalance);
        userRepository.save(user);
        return newBalance;
    }
}
