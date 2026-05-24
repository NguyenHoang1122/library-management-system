package com.librarymanagementsystem.repository.borrow;

import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.model.borrow.ReturnRequest;

import com.librarymanagementsystem.model.borrow.status.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    //yêu cầu trả truyện dựa trên trạng thái yêu cầu cụ thể
//    List<ReturnRequest> findByRequestStatus(RequestStatus status);
    // Yêu cầu trả truyện có trạng thái nằm trong danh sách các trạng thái truyền vào
    List<ReturnRequest> findByRequestStatusInOrderByRequestDateDesc(List<RequestStatus> statuses);
    //các yêu cầu trả truyện theo đối tượng giao dịch mượn
    List<ReturnRequest> findByBorrowTransactionOrderByRequestDateDesc(BorrowTransaction transaction);

    @Query("SELECT DISTINCT rr FROM ReturnRequest rr " +
           "LEFT JOIN rr.user u " +
           "WHERE rr.requestStatus IN (com.librarymanagementsystem.model.borrow.status.RequestStatus.PENDING, com.librarymanagementsystem.model.borrow.status.RequestStatus.APPROVED, com.librarymanagementsystem.model.borrow.status.RequestStatus.RETURNING) AND " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(u.userName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))" +
           "ORDER BY rr.requestDate DESC")
    Page<ReturnRequest> findPendingReturnRequestsWithSearch(@Param("query") String query, Pageable pageable);
}

