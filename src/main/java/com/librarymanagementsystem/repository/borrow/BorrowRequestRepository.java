package com.librarymanagementsystem.repository.borrow;

import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {

    //yêu cầu mượn theo đối tượng User
    List<BorrowRequest> findByUserOrderByRequestDateDesc(User user);

    Page<BorrowRequest> findByUser(User user, Pageable pageable);

    //yêu cầu mượn chưa duyệt
    @Query("SELECT br FROM BorrowRequest br WHERE br.requestStatus = 'PENDING' ORDER BY br.requestDate DESC")
    List<BorrowRequest> findPendingRequests();

    @Query("SELECT DISTINCT br FROM BorrowRequest br " +
           "LEFT JOIN br.user u " +
           "WHERE br.requestStatus = com.librarymanagementsystem.model.borrow.status.RequestStatus.PENDING AND " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(u.userName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))" +
           "ORDER BY br.requestDate DESC")
    Page<BorrowRequest> findPendingRequestsWithSearch(@Param("query") String query, Pageable pageable);
}