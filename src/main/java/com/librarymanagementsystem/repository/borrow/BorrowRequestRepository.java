package com.librarymanagementsystem.repository.borrow;

import com.librarymanagementsystem.model.borrow.BorrowRequest;
import com.librarymanagementsystem.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {

    //yêu cầu mượn theo đối tượng User
    List<BorrowRequest> findByUserOrderByRequestDateDesc(User user);

    Page<BorrowRequest> findByUser(User user, Pageable pageable);

    //yêu cầu mượn chưa duyệt
    @Query("SELECT br FROM BorrowRequest br WHERE br.requestStatus = com.librarymanagementsystem.model.borrow.status.RequestStatus.PENDING ORDER BY br.requestDate DESC")
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

    @Query("SELECT DISTINCT br FROM BorrowRequest br " +
           "LEFT JOIN br.user u " +
           "WHERE br.requestStatus = com.librarymanagementsystem.model.borrow.status.RequestStatus.APPROVED AND " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(u.userName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))" +
           "ORDER BY br.requestDate DESC")
    Page<BorrowRequest> findApprovedRequestsWithSearch(@Param("query") String query, Pageable pageable);

    @Query("SELECT SUM(br.totalDeposit + br.shippingFee) FROM BorrowRequest br WHERE br.requestStatus IN (com.librarymanagementsystem.model.borrow.status.RequestStatus.APPROVED, com.librarymanagementsystem.model.borrow.status.RequestStatus.COMPLETED, com.librarymanagementsystem.model.borrow.status.RequestStatus.RETURNING) AND MONTH(br.requestDate) = MONTH(CURRENT_DATE) AND YEAR(br.requestDate) = YEAR(CURRENT_DATE)")
    BigDecimal sumTotalRevenueCurrentMonth();

    @Query("SELECT SUM(bri.quantity * b.importPrice) FROM BorrowRequestItem bri JOIN bri.borrowRequest br JOIN bri.book b WHERE br.requestStatus IN (com.librarymanagementsystem.model.borrow.status.RequestStatus.APPROVED, com.librarymanagementsystem.model.borrow.status.RequestStatus.COMPLETED, com.librarymanagementsystem.model.borrow.status.RequestStatus.RETURNING) AND MONTH(br.requestDate) = MONTH(CURRENT_DATE) AND YEAR(br.requestDate) = YEAR(CURRENT_DATE)")
    BigDecimal sumTotalCostCurrentMonth();

    @Query(value = "SELECT DATE(request_date) as date, SUM(total_deposit + shipping_fee) as revenue FROM borrow_requests WHERE request_status IN ('APPROVED', 'COMPLETED', 'RETURNING') GROUP BY DATE(request_date) ORDER BY date DESC LIMIT 30", nativeQuery = true)
    List<Object[]> getDailyRevenue();

    @Query(value = "SELECT DATE_FORMAT(request_date, '%Y') as date, SUM(total_deposit + shipping_fee) as revenue FROM borrow_requests WHERE request_status IN ('APPROVED', 'COMPLETED', 'RETURNING') GROUP BY DATE_FORMAT(request_date, '%Y') ORDER BY date DESC LIMIT 5", nativeQuery = true)
    List<Object[]> getYearlyRevenue();

    @Query(value = "SELECT DATE_FORMAT(request_date, '%Y-%m') as date, SUM(total_deposit + shipping_fee) as revenue FROM borrow_requests WHERE request_status IN ('APPROVED', 'COMPLETED', 'RETURNING') GROUP BY DATE_FORMAT(request_date, '%Y-%m') ORDER BY date DESC LIMIT 12", nativeQuery = true)
    List<Object[]> getMonthlyRevenue();
}