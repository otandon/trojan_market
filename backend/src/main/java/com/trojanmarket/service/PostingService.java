package com.trojanmarket.service;

import com.trojanmarket.dto.CreatePostingRequest;
import com.trojanmarket.dto.EditPostingRequest;
import com.trojanmarket.entity.NotificationType;
import com.trojanmarket.entity.Posting;
import com.trojanmarket.entity.PostingPhoto;
import com.trojanmarket.entity.PostingStatus;
import com.trojanmarket.entity.Transaction;
import com.trojanmarket.repository.PostingPhotoRepository;
import com.trojanmarket.repository.PostingRepository;
import com.trojanmarket.repository.TransactionRepository;
import com.trojanmarket.security.ForbiddenException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PostingService {

    private static final int MAX_PHOTOS = 8;

    private final PostingRepository postingRepository;
    private final PostingPhotoRepository photoRepository;
    private final TransactionRepository transactionRepository;
    private final AuthService authService;
    private final NotificationService notificationService;

    public PostingService(PostingRepository postingRepository,
                          PostingPhotoRepository photoRepository,
                          TransactionRepository transactionRepository,
                          AuthService authService,
                          NotificationService notificationService) {
        this.postingRepository = postingRepository;
        this.photoRepository = photoRepository;
        this.transactionRepository = transactionRepository;
        this.authService = authService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Posting createPosting(Integer sellerID, CreatePostingRequest req) {
        if (sellerID == null) {
            throw new ForbiddenException("Guests cannot create postings");
        }
        if (authService.isBannedUser(sellerID)) {
            throw new ForbiddenException("High-risk users cannot create postings");
        }

        Posting posting = Posting.builder()
                .sellerID(sellerID)
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory())
                .status(PostingStatus.AVAILABLE)
                .price(req.getPrice())
                .isActive(true)
                .build();

        Posting saved = postingRepository.save(posting);

        if (req.getPhotos() != null && !req.getPhotos().isEmpty()) {
            if (req.getPhotos().size() > MAX_PHOTOS) {
                throw new IllegalArgumentException("Listings may not have more than " + MAX_PHOTOS + " photos");
            }
            int order = 0;
            for (String dataUrl : req.getPhotos()) {
                if (dataUrl == null || dataUrl.isBlank()) continue;
                photoRepository.save(PostingPhoto.builder()
                        .postID(saved.getPostID())
                        .photoData(dataUrl)
                        .sortOrder(order++)
                        .build());
            }
        }

        // TODO (task #7 — Notifications): NotificationService.notifyInterestedUsers(saved)
        //   for any user with a saved search or watched category.

        return saved;
    }

    @Transactional
    public Posting editPosting(Integer postID, Integer callerID, EditPostingRequest req) {
        Posting posting = ownedPosting(postID, callerID);

        if (req.getTitle() != null) posting.setTitle(req.getTitle());
        if (req.getDescription() != null) posting.setDescription(req.getDescription());
        if (req.getCategory() != null) posting.setCategory(req.getCategory());
        if (req.getPrice() != null) posting.setPrice(req.getPrice());

        if (req.getStatus() != null && req.getStatus() != posting.getStatus()) {
            applyStatusTransition(posting, req);
        }

        return postingRepository.save(posting);
    }

    @Transactional
    public void softDelete(Integer postID, Integer callerID) {
        Posting posting = ownedPosting(postID, callerID);
        posting.setIsActive(false);
        postingRepository.save(posting);
    }

    public List<Posting> getActiveByseller(Integer sellerID) {
        return postingRepository.findBySellerIDAndIsActiveTrue(sellerID);
    }

    public Posting getById(Integer postID) {
        return postingRepository.findById(postID)
                .orElseThrow(() -> new EntityNotFoundException("Posting not found: " + postID));
    }

    // --- internals ----------------------------------------------------------

    private Posting ownedPosting(Integer postID, Integer callerID) {
        Posting posting = getById(postID);
        if (callerID == null || !posting.getSellerID().equals(callerID)) {
            throw new ForbiddenException("Only the seller can modify this posting");
        }
        return posting;
    }

    private void applyStatusTransition(Posting posting, EditPostingRequest req) {
        PostingStatus from = posting.getStatus();
        PostingStatus to = req.getStatus();

        boolean valid = switch (from) {
            case AVAILABLE -> to == PostingStatus.PENDING || to == PostingStatus.SOLD;
            case PENDING -> to == PostingStatus.AVAILABLE || to == PostingStatus.SOLD;
            case SOLD -> false; // SOLD is terminal
        };
        if (!valid) {
            throw new IllegalArgumentException("Invalid status transition: " + from + " -> " + to);
        }

        posting.setStatus(to);

        if (to == PostingStatus.SOLD) {
            if (req.getBuyerID() == null) {
                throw new IllegalArgumentException("buyerID is required when transitioning to SOLD");
            }
            if (req.getBuyerID().equals(posting.getSellerID())) {
                throw new IllegalArgumentException("Buyer and seller cannot be the same user");
            }

            Transaction tx = Transaction.builder()
                    .postID(posting.getPostID())
                    .buyerID(req.getBuyerID())
                    .sellerID(posting.getSellerID())
                    .salePrice(req.getSalePrice() != null ? req.getSalePrice() : posting.getPrice())
                    .build();
            transactionRepository.save(tx);

            notificationService.createNotification(
                    req.getBuyerID(), NotificationType.ITEM_PURCHASED, posting.getPostID(), null);
            notificationService.createNotification(
                    posting.getSellerID(), NotificationType.ITEM_SOLD, posting.getPostID(), null);
        }
    }
}
