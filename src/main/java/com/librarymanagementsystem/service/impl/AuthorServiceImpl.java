package com.librarymanagementsystem.service.impl;

import com.librarymanagementsystem.model.user.Author;
import com.librarymanagementsystem.repository.AuthorRepository;
import com.librarymanagementsystem.service.AuthorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {
    private final AuthorRepository authorRepository;

    @Override
    public List<Author> getAllAuthors() {
        return authorRepository.findAll();
    }

    @Override
    public Optional<Author> getAuthorById(Long id) {
        return authorRepository.findById(id);
    }

    @Override
    public Author saveAuthor(Author author) {
        return authorRepository.save(author);
    }

    @Override
    public Author updateAuthor(Long id, Author author) {
        Author existing = authorRepository.findById(id).orElseThrow(() -> new RuntimeException("Tác giả không tồn tại"));
        existing.setName(author.getName());
        return authorRepository.save(existing);
    }

    @Override
    public void deleteAuthor(Long id) {
        authorRepository.deleteById(id);
    }

}
