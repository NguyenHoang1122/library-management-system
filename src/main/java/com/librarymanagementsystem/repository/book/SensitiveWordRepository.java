package com.librarymanagementsystem.repository.book;

import com.librarymanagementsystem.model.book.SensitiveWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SensitiveWordRepository extends JpaRepository<SensitiveWord, Long> {
    boolean existsByWordIgnoreCase(String word);
}
