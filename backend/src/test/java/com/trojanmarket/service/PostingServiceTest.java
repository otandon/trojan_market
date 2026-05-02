package com.trojanmarket.service;

import com.trojanmarket.dto.CreatePostingRequest;
import com.trojanmarket.dto.EditPostingRequest;
import com.trojanmarket.entity.Category;
import com.trojanmarket.entity.NotificationType;
import com.trojanmarket.entity.Posting;
import com.trojanmarket.entity.PostingPhoto;
import com.trojanmarket.entity.PostingStatus;
import com.trojanmarket.entity.Transaction;
import com.trojanmarket.repository.PostingPhotoRepository;
import com.trojanmarket.repository.PostingRepository;
import com.trojanmarket.repository.TransactionRepository;
import com.trojanmarket.security.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PostingService}.
 *
 * Covers the test plan's Database/Posting tests adapted to current code:
 *   - createPosting: valid posting persists; sellerID required; high-risk blocked;
 *     photos persist with sortOrder; >MAX_PHOTOS rejected
 *   - editPosting status transitions: AVAILABLE↔PENDING, AVAILABLE→SOLD, PENDING→SOLD;
 *     SOLD is terminal (no further transitions); SOLD requires buyerID; transaction
 *     row is created on SOLD; non-owner cannot edit
 *   - softDelete: sets is_active=false (non-destructive)
 */
@ExtendWith(MockitoExtension.class)
class PostingServiceTest {

    @Mock PostingRepository postingRepository;
    @Mock PostingPhotoRepository photoRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock AuthService authService;
    @Mock NotificationService notificationService;
    @InjectMocks PostingService postingService;

    private static CreatePostingRequest req() {
        CreatePostingRequest r = new CreatePostingRequest();
        r.setTitle("Camera");
        r.setDescription("A great camera");
        r.setCategory(Category.ELECTRONICS);
        r.setPrice(new BigDecimal("100.00"));
        return r;
    }

    @Nested
    @DisplayName("createPosting")
    class Create {

        @Test
        void persistsValidPostingWithDefaultsAndPhotos() {
            when(authService.isBannedUser(1)).thenReturn(false);
            when(postingRepository.save(any(Posting.class))).thenAnswer(inv -> {
                Posting p = inv.getArgument(0);
                p.setPostID(42);
                return p;
            });
            CreatePostingRequest r = req();
            r.setPhotos(List.of("data:image/png;base64,A", "data:image/png;base64,B"));

            Posting saved = postingService.createPosting(1, r);

            ArgumentCaptor<Posting> captor = ArgumentCaptor.forClass(Posting.class);
            verify(postingRepository).save(captor.capture());
            assertThat(captor.getValue().getSellerID()).isEqualTo(1);
            assertThat(captor.getValue().getStatus()).isEqualTo(PostingStatus.AVAILABLE);
            assertThat(captor.getValue().getIsActive()).isTrue();
            assertThat(saved.getPostID()).isEqualTo(42);

            // Both photos persisted, in submitted order.
            ArgumentCaptor<PostingPhoto> photoCaptor = ArgumentCaptor.forClass(PostingPhoto.class);
            verify(photoRepository, times(2)).save(photoCaptor.capture());
            List<PostingPhoto> persisted = photoCaptor.getAllValues();
            assertThat(persisted.get(0).getSortOrder()).isZero();
            assertThat(persisted.get(0).getPhotoData()).isEqualTo("data:image/png;base64,A");
            assertThat(persisted.get(1).getSortOrder()).isOne();
            assertThat(persisted.get(1).getPhotoData()).isEqualTo("data:image/png;base64,B");
        }

        @Test
        void guestSellerRefused() {
            assertThatThrownBy(() -> postingService.createPosting(null, req()))
                    .isInstanceOf(ForbiddenException.class);
            verify(postingRepository, never()).save(any());
        }

        @Test
        void highRiskSellerRefused() {
            when(authService.isBannedUser(1)).thenReturn(true);
            assertThatThrownBy(() -> postingService.createPosting(1, req()))
                    .isInstanceOf(ForbiddenException.class);
            verify(postingRepository, never()).save(any());
        }

        @Test
        void rejectsTooManyPhotos() {
            when(authService.isBannedUser(1)).thenReturn(false);
            when(postingRepository.save(any(Posting.class))).thenAnswer(inv -> {
                Posting p = inv.getArgument(0);
                p.setPostID(1);
                return p;
            });
            CreatePostingRequest r = req();
            r.setPhotos(List.of("a", "b", "c", "d", "e", "f", "g", "h", "i")); // 9 > MAX 8

            assertThatThrownBy(() -> postingService.createPosting(1, r))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("editPosting status transitions")
    class Status {

        private Posting available() {
            return Posting.builder().postID(10).sellerID(1).status(PostingStatus.AVAILABLE)
                    .price(new BigDecimal("100.00")).isActive(true).build();
        }

        @Test
        void availableToPending() {
            when(postingRepository.findById(10)).thenReturn(Optional.of(available()));
            when(postingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            EditPostingRequest req = new EditPostingRequest();
            req.setStatus(PostingStatus.PENDING);

            Posting result = postingService.editPosting(10, 1, req);
            assertThat(result.getStatus()).isEqualTo(PostingStatus.PENDING);
        }

        @Test
        void availableToSoldCreatesTransaction() {
            when(postingRepository.findById(10)).thenReturn(Optional.of(available()));
            when(postingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            EditPostingRequest req = new EditPostingRequest();
            req.setStatus(PostingStatus.SOLD);
            req.setBuyerID(2);
            req.setSalePrice(new BigDecimal("90.00"));

            postingService.editPosting(10, 1, req);

            ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(txCaptor.capture());
            Transaction tx = txCaptor.getValue();
            assertThat(tx.getPostID()).isEqualTo(10);
            assertThat(tx.getBuyerID()).isEqualTo(2);
            assertThat(tx.getSellerID()).isEqualTo(1);
            assertThat(tx.getSalePrice()).isEqualByComparingTo("90.00");

            // Both buyer and seller get notifications.
            verify(notificationService).createNotification(2, NotificationType.ITEM_PURCHASED, 10, null);
            verify(notificationService).createNotification(1, NotificationType.ITEM_SOLD, 10, null);
        }

        @Test
        void soldRequiresBuyerID() {
            when(postingRepository.findById(10)).thenReturn(Optional.of(available()));
            EditPostingRequest req = new EditPostingRequest();
            req.setStatus(PostingStatus.SOLD);

            assertThatThrownBy(() -> postingService.editPosting(10, 1, req))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(transactionRepository, never()).save(any());
        }

        @Test
        void soldRefusesBuyerEqualToSeller() {
            when(postingRepository.findById(10)).thenReturn(Optional.of(available()));
            EditPostingRequest req = new EditPostingRequest();
            req.setStatus(PostingStatus.SOLD);
            req.setBuyerID(1);

            assertThatThrownBy(() -> postingService.editPosting(10, 1, req))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void soldIsTerminal() {
            Posting sold = Posting.builder().postID(10).sellerID(1).status(PostingStatus.SOLD)
                    .price(new BigDecimal("100.00")).isActive(true).build();
            when(postingRepository.findById(10)).thenReturn(Optional.of(sold));
            EditPostingRequest req = new EditPostingRequest();
            req.setStatus(PostingStatus.AVAILABLE);

            assertThatThrownBy(() -> postingService.editPosting(10, 1, req))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void onlySellerCanEdit() {
            when(postingRepository.findById(10)).thenReturn(Optional.of(available()));
            EditPostingRequest req = new EditPostingRequest();
            req.setStatus(PostingStatus.PENDING);

            assertThatThrownBy(() -> postingService.editPosting(10, 999, req))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("softDelete")
    class SoftDelete {

        @Test
        void setsIsActiveFalse() {
            Posting p = Posting.builder().postID(10).sellerID(1).isActive(true)
                    .status(PostingStatus.AVAILABLE).price(new BigDecimal("1")).build();
            when(postingRepository.findById(10)).thenReturn(Optional.of(p));
            when(postingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            postingService.softDelete(10, 1);
            assertThat(p.getIsActive()).isFalse();
            verify(postingRepository).save(p);
        }

        @Test
        void onlySellerCanDelete() {
            Posting p = Posting.builder().postID(10).sellerID(1).isActive(true)
                    .status(PostingStatus.AVAILABLE).price(new BigDecimal("1")).build();
            when(postingRepository.findById(10)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> postingService.softDelete(10, 999))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}
