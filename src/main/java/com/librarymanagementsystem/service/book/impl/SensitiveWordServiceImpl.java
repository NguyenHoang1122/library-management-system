package com.librarymanagementsystem.service.book.impl;

import com.librarymanagementsystem.model.book.SensitiveWord;
import com.librarymanagementsystem.repository.book.SensitiveWordRepository;
import com.librarymanagementsystem.service.book.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private final SensitiveWordRepository sensitiveWordRepository;

    @Override
    public List<SensitiveWord> getAllSensitiveWords() {
        return sensitiveWordRepository.findAll();
    }

    @Override
    public boolean existsByWord(String word) {
        return sensitiveWordRepository.existsByWordIgnoreCase(word);
    }

    @Override
    public SensitiveWord addSensitiveWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            throw new RuntimeException("Từ không được để trống!");
        }
        String cleanWord = word.trim().toLowerCase();
        if (existsByWord(cleanWord)) {
            throw new RuntimeException("Từ này đã tồn tại trong danh sách!");
        }
        return sensitiveWordRepository.save(new SensitiveWord(null, cleanWord));
    }

    @Override
    public void deleteSensitiveWord(Long id) {
        sensitiveWordRepository.deleteById(id);
    }
}
