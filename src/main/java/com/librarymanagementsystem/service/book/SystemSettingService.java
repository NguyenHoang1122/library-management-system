package com.librarymanagementsystem.service.book;

public interface SystemSettingService {
    String getSetting(String key, String defaultValue);
    void saveSetting(String key, String value);
    boolean isAiModerationEnabled();
    String getAiApiKey();
    String getAiModerationAction();
}
