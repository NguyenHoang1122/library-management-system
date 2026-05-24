package com.librarymanagementsystem.repository.book.category;

import com.librarymanagementsystem.model.book.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Page<Category> findByCategoryNameContainingIgnoreCase(String categoryName, Pageable pageable);
}
