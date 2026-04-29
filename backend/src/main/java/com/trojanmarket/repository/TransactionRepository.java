package com.trojanmarket.repository;

import com.trojanmarket.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findByBuyerIDOrderByTransactionTimeDesc(Integer buyerID);

    List<Transaction> findBySellerIDOrderByTransactionTimeDesc(Integer sellerID);
}
