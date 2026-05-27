package com.librarymanagementsystem.repository.user;

import com.librarymanagementsystem.model.user.AdminWalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminWalletTransactionRepository extends JpaRepository<AdminWalletTransaction, Long> {

    Page<AdminWalletTransaction> findAllByOrderByTransactionDateDesc(Pageable pageable);

    @Query("SELECT SUM(t.amount) FROM AdminWalletTransaction t WHERE t.amount > 0 AND MONTH(t.transactionDate) = :month AND YEAR(t.transactionDate) = :year")
    Double sumTotalRevenueByMonth(@Param("month") int month, @Param("year") int year);

    @Query("SELECT SUM(ABS(t.amount)) FROM AdminWalletTransaction t WHERE t.amount < 0 AND MONTH(t.transactionDate) = :month AND YEAR(t.transactionDate) = :year")
    Double sumTotalExpenseByMonth(@Param("month") int month, @Param("year") int year);

    @Query(value = "SELECT DATE_FORMAT(transaction_date, '%Y-%m') as date, " +
            "SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END) as revenue, " +
            "SUM(CASE WHEN amount < 0 THEN ABS(amount) ELSE 0 END) as expense " +
            "FROM admin_wallet_transactions " +
            "GROUP BY DATE_FORMAT(transaction_date, '%Y-%m') " +
            "ORDER BY date DESC LIMIT 12", nativeQuery = true)
    List<Object[]> getMonthlyFinanceChartData();

    @Query(value = "SELECT DATE(transaction_date) as date, " +
            "SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END) as revenue, " +
            "SUM(CASE WHEN amount < 0 THEN ABS(amount) ELSE 0 END) as expense " +
            "FROM admin_wallet_transactions " +
            "GROUP BY DATE(transaction_date) " +
            "ORDER BY date DESC LIMIT 30", nativeQuery = true)
    List<Object[]> getDailyFinanceChartData();
}
