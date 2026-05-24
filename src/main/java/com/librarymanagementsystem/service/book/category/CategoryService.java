package com.librarymanagementsystem.service.book.category;

import com.librarymanagementsystem.model.book.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CategoryService {

    List<Category> getAllCategories();

    Page<Category> getAllCategories(Pageable pageable);

    Page<Category> searchCategories(String query, Pageable pageable);

    Optional<Category> getCategoryById(Long id);

    Category saveCategory(Category category);

    Category updateCategory(Long id, Category category);

    void deleteCategory(Long id);
}
