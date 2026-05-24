package com.librarymanagementsystem.repository.borrow;

import com.librarymanagementsystem.model.borrow.ReturnRequest;
import com.librarymanagementsystem.model.borrow.ReturnRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestItemRepository extends JpaRepository<ReturnRequestItem, Long> {
    List<ReturnRequestItem> findByReturnRequest(ReturnRequest returnRequest);
}
