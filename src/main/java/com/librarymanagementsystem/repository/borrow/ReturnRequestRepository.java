package com.librarymanagementsystem.repository.borrow;

import com.librarymanagementsystem.model.borrow.ReturnRequest;
import com.librarymanagementsystem.model.borrow.status.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    List<ReturnRequest> findByRequestStatus(RequestStatus status);
    List<ReturnRequest> findByRequestStatusInOrderByRequestDateDesc(List<RequestStatus> statuses);
    List<ReturnRequest> findByBorrowTransactionOrderByRequestDateDesc(com.librarymanagementsystem.model.borrow.BorrowTransaction transaction);
}
