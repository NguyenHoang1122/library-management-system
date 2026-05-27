package com.librarymanagementsystem.service.book.impl;

import com.librarymanagementsystem.model.book.SystemSetting;
import com.librarymanagementsystem.repository.book.SystemSettingRepository;
import com.librarymanagementsystem.service.book.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    @Override
    public String getSetting(String key, String defaultValue) {
        return systemSettingRepository.findById(key)
                .map(SystemSetting::getValue)
                .orElse(defaultValue);
    }

    @Override
    public void saveSetting(String key, String value) {
        SystemSetting setting = systemSettingRepository.findById(key)
                .orElse(new SystemSetting(key, value));
        setting.setValue(value);
        systemSettingRepository.save(setting);
    }

    @Override
    public boolean isAiModerationEnabled() {
        return "true".equalsIgnoreCase(getSetting("ai_moderation_enabled", "false"));
    }

    @Override
    public String getAiApiKey() {
        return getSetting("ai_api_key", "");
    }

    @Override
    public String getAiModerationAction() {
        return getSetting("ai_moderation_action", "CENSOR");
    }
}
