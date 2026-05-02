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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock UserRepository userRepository;
    @Mock PostingRepository postingRepository;
    @InjectMocks ReviewService reviewService;

    @Nested
    @DisplayName("createReview")
    class Create {

        private Transaction tx() {
            return Transaction.builder()
                    .transactionID(7).postID(10).buyerID(2).sellerID(1).build();
        }

        @Test
        void persistsReviewAndIncrementsSellerRatingTotals() {
            when(transactionRepository.findById(7)).thenReturn(Optional.of(tx()));
            when(reviewRepository.findByTransactionID(7)).thenReturn(Optional.empty());
            when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
                Review r = inv.getArgument(0);
                r.setReviewID(99);
                r.setReviewTime(LocalDateTime.now());
                return r;
            });
            User seller = User.builder().userID(1).review(8).reviewCount(2).build();
            when(userRepository.findById(1)).thenReturn(Optional.of(seller));
            when(userRepository.findById(2)).thenReturn(Optional.of(
                    User.builder().userID(2).firstName("Bea").lastName("Buyer").build()));
            when(postingRepository.findById(10)).thenReturn(Optional.of(
                    Posting.builder().postID(10).title("Camera").build()));

            CreateReviewRequest req = new CreateReviewRequest(7, (short) 5, "Great seller!");
            ReviewDTO out = reviewService.createReview(2, req);

            assertThat(out.getReviewID()).isEqualTo(99);
            assertThat(out.getRating()).isEqualTo((short) 5);
            assertThat(out.getReviewerFirstName()).isEqualTo("Bea");
            assertThat(out.getPostTitle()).isEqualTo("Camera");

            // Seller's running totals updated for the average calculation elsewhere.
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getReview()).isEqualTo(13); // 8 + 5
            assertThat(userCaptor.getValue().getReviewCount()).isEqualTo(3);
        }

        @Test
        void onlyBuyerCanReviewTransaction() {
            when(transactionRepository.findById(7)).thenReturn(Optional.of(tx()));
            CreateReviewRequest req = new CreateReviewRequest(7, (short) 5, "");

            // Caller is the seller, not the buyer → ForbiddenException
            assertThatThrownBy(() -> reviewService.createReview(1, req))
                    .isInstanceOf(ForbiddenException.class);
            verify(reviewRepository, never()).save(any());
        }

        @Test
        void duplicateReviewRefused() {
            when(transactionRepository.findById(7)).thenReturn(Optional.of(tx()));
            when(reviewRepository.findByTransactionID(7)).thenReturn(Optional.of(
                    Review.builder().reviewID(1).build()));

            CreateReviewRequest req = new CreateReviewRequest(7, (short) 5, "");
            assertThatThrownBy(() -> reviewService.createReview(2, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        void unknownTransactionThrows() {
            when(transactionRepository.findById(7)).thenReturn(Optional.empty());
            CreateReviewRequest req = new CreateReviewRequest(7, (short) 5, "");

            assertThatThrownBy(() -> reviewService.createReview(2, req))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        void unauthenticatedRefused() {
            CreateReviewRequest req = new CreateReviewRequest(7, (short) 5, "");
            assertThatThrownBy(() -> reviewService.createReview(null, req))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("listForSeller")
    class List_ {

        @Test
        void returnsEnrichedReviewsForSeller() {
            Review r1 = Review.builder().reviewID(1).reviewerID(2).sellerID(1).transactionID(7)
                    .rating((short) 5).comment("great").reviewTime(LocalDateTime.now()).build();
            when(reviewRepository.findBySellerIDOrderByReviewTimeDesc(1)).thenReturn(List.of(r1));
            when(userRepository.findAllById(any())).thenReturn(List.of(
                    User.builder().userID(2).firstName("Bea").lastName("Buyer").build()));
            when(transactionRepository.findAllById(any())).thenReturn(List.of(
                    Transaction.builder().transactionID(7).postID(10).buyerID(2).sellerID(1).build()));
            when(postingRepository.findAllById(any())).thenReturn(List.of(
                    Posting.builder().postID(10).title("Camera").build()));

            java.util.List<ReviewDTO> reviews = reviewService.listForSeller(1);

            assertThat(reviews).hasSize(1);
            assertThat(reviews.get(0).getReviewerFirstName()).isEqualTo("Bea");
            assertThat(reviews.get(0).getPostTitle()).isEqualTo("Camera");
            assertThat(reviews.get(0).getRating()).isEqualTo((short) 5);
        }

        @Test
        void emptyForSellerWithNoReviews() {
            when(reviewRepository.findBySellerIDOrderByReviewTimeDesc(1)).thenReturn(List.of());
            assertThat(reviewService.listForSeller(1)).isEmpty();
        }
    }
}
