package com.librarymanagementsystem.service.book;

import com.librarymanagementsystem.model.book.SensitiveWord;
import java.util.List;

public interface SensitiveWordService {
    List<SensitiveWord> getAllSensitiveWords();
    boolean existsByWord(String word);
    SensitiveWord addSensitiveWord(String word);
    void deleteSensitiveWord(Long id);
}
