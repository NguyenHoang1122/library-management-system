package com.librarymanagementsystem.repository.book;

import com.librarymanagementsystem.model.book.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}
