package com.librarymanagementsystem.repository.borrow;

import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.BorrowRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowRequestItemRepository extends JpaRepository<BorrowRequestItem, Long> {

    //danh sách các cuốn truyện một yêu cầu mượn cụ thể
    List<BorrowRequestItem> findByBorrowRequest(BorrowRequest borrowRequest);

}
