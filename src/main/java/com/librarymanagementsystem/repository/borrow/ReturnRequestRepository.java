package com.librarymanagementsystem.repository.borrow;

import com.librarymanagementsystem.model.borrow.BorrowTransaction;
import com.librarymanagementsystem.model.borrow.ReturnRequest;

import com.librarymanagementsystem.model.borrow.status.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
}

