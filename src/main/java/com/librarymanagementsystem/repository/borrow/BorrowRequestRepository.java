package com.librarymanagementsystem.repository.borrow;

import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.borrow.status.RequestStatus;
import com.librarymanagementsystem.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {

    // Lấy danh sách yêu cầu mượn của user
    List<BorrowRequest> findByUser(User user);

    // Lấy danh sách yêu cầu mượn chưa duyệt
    @Query("SELECT br FROM BorrowRequest br WHERE br.requestStatus = 'PENDING' ORDER BY br.requestDate DESC")
    List<BorrowRequest> findPendingRequests();
}