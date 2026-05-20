package com.librarymanagementsystem.repository.borrow;

import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {

    //yêu cầu mượn theo đối tượng User
    List<BorrowRequest> findByUserOrderByRequestDateDesc(User user);

    //yêu cầu mượn chưa duyệt
    @Query("SELECT br FROM BorrowRequest br WHERE br.requestStatus = 'PENDING' ORDER BY br.requestDate DESC")
    List<BorrowRequest> findPendingRequests();
}