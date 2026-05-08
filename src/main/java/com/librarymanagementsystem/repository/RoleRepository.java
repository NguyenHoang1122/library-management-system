package com.librarymanagementsystem.repository;

import com.librarymanagementsystem.model.user.Role;
import com.librarymanagementsystem.model.user.status.RoleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(RoleStatus roleName);
}
