package com.trojanmarket.service;

import com.trojanmarket.dto.CreateReviewRequest;
import com.trojanmarket.dto.ReviewDTO;
import com.trojanmarket.entity.Posting;
import com.trojanmarket.entity.Review;
import com.trojanmarket.entity.Transaction;
import com.trojanmarket.entity.User;
import com.trojanmarket.repository.PostingRepository;
import com.trojanmarket.repository.ReviewRepository;
import com.trojanmarket.repository.TransactionRepository;
import com.trojanmarket.repository.UserRepository;
import com.trojanmarket.security.ForbiddenException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PostingRepository postingRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         TransactionRepository transactionRepository,
                         UserRepository userRepository,
                         PostingRepository postingRepository) {
        this.reviewRepository = reviewRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.postingRepository = postingRepository;
    }

    public List<ReviewDTO> listForSeller(Integer sellerID) {
        List<Review> reviews = reviewRepository.findBySellerIDOrderByReviewTimeDesc(sellerID);
        if (reviews.isEmpty()) {
            return List.of();
        }

        Set<Integer> reviewerIDs = new HashSet<>();
        Set<Integer> transactionIDs = new HashSet<>();
        for (Review r : reviews) {
            reviewerIDs.add(r.getReviewerID());
            transactionIDs.add(r.getTransactionID());
        }

        Map<Integer, User> reviewersByID = userRepository.findAllById(reviewerIDs).stream()
                .collect(Collectors.toMap(User::getUserID, u -> u));
        Map<Integer, Transaction> txByID = transactionRepository.findAllById(transactionIDs).stream()
                .collect(Collectors.toMap(Transaction::getTransactionID, t -> t));
        Set<Integer> postIDs = txByID.values().stream().map(Transaction::getPostID).collect(Collectors.toSet());
        Map<Integer, Posting> postingsByID = postingRepository.findAllById(postIDs).stream()
                .collect(Collectors.toMap(Posting::getPostID, p -> p));

        return reviews.stream().map(r -> {
            User reviewer = reviewersByID.get(r.getReviewerID());
            Transaction tx = txByID.get(r.getTransactionID());
            Posting posting = tx == null ? null : postingsByID.get(tx.getPostID());
            return ReviewDTO.builder()
                    .reviewID(r.getReviewID())
                    .reviewerID(r.getReviewerID())
                    .reviewerFirstName(reviewer == null ? null : reviewer.getFirstName())
                    .reviewerLastName(reviewer == null ? null : reviewer.getLastName())
                    .sellerID(r.getSellerID())
                    .transactionID(r.getTransactionID())
                    .postID(tx == null ? null : tx.getPostID())
                    .postTitle(posting == null ? null : posting.getTitle())
                    .rating(r.getRating())
                    .comment(r.getComment())
                    .reviewTime(r.getReviewTime())
                    .build();
        }).toList();
    }

    @Transactional
    public ReviewDTO createReview(Integer reviewerID, CreateReviewRequest req) {
        if (reviewerID == null) {
            throw new ForbiddenException("Authentication required");
        }

        Transaction tx = transactionRepository.findById(req.getTransactionID())
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + req.getTransactionID()));

        if (!tx.getBuyerID().equals(reviewerID)) {
            throw new ForbiddenException("Only the buyer of a transaction can leave a review for it");
        }
        if (reviewRepository.findByTransactionID(tx.getTransactionID()).isPresent()) {
            throw new IllegalArgumentException("A review already exists for this transaction");
        }

        Review saved = reviewRepository.save(Review.builder()
                .reviewerID(reviewerID)
                .sellerID(tx.getSellerID())
                .transactionID(tx.getTransactionID())
                .rating(req.getRating())
                .comment(req.getComment())
                .build());

        // Update the seller's running rating totals so the average shown elsewhere
        // (PostingDetail.sellerRating, etc.) stays in sync.
        User seller = userRepository.findById(tx.getSellerID())
                .orElseThrow(() -> new EntityNotFoundException("Seller not found: " + tx.getSellerID()));
        seller.setReview((seller.getReview() == null ? 0 : seller.getReview()) + req.getRating());
        seller.setReviewCount((seller.getReviewCount() == null ? 0 : seller.getReviewCount()) + 1);
        userRepository.save(seller);

        Posting posting = postingRepository.findById(tx.getPostID()).orElse(null);
        User reviewer = userRepository.findById(reviewerID).orElse(null);

        return ReviewDTO.builder()
                .reviewID(saved.getReviewID())
                .reviewerID(saved.getReviewerID())
                .reviewerFirstName(reviewer == null ? null : reviewer.getFirstName())
                .reviewerLastName(reviewer == null ? null : reviewer.getLastName())
                .sellerID(saved.getSellerID())
                .transactionID(saved.getTransactionID())
                .postID(tx.getPostID())
                .postTitle(posting == null ? null : posting.getTitle())
                .rating(saved.getRating())
                .comment(saved.getComment())
                .reviewTime(saved.getReviewTime())
                .build();
    }
}
