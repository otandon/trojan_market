package com.trojanmarket.repository;

import com.trojanmarket.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findBySellerIDOrderByReviewTimeDesc(Integer sellerID);

    Optional<Review> findByTransactionID(Integer transactionID);
}
