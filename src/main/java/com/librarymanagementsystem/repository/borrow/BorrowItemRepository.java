package com.librarymanagementsystem.repository.borrow;

import com.librarymanagementsystem.model.borrow.BorrowItem;
import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface BorrowItemRepository extends JpaRepository<BorrowItem, Long> {

    // Tìm kiếm và lấy danh sách các cuốn truyện
    List<BorrowItem> findByTransaction(BorrowTransaction transaction);
}
