package com.trojanmarket.controller;

import com.trojanmarket.dto.CreateReviewRequest;
import com.trojanmarket.dto.ReviewDTO;
import com.trojanmarket.security.SecurityUtils;
import com.trojanmarket.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/seller/{sellerID}")
    public ResponseEntity<List<ReviewDTO>> listForSeller(@PathVariable Integer sellerID) {
        return ResponseEntity.ok(reviewService.listForSeller(sellerID));
    }

    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(@Valid @RequestBody CreateReviewRequest req) {
        Integer reviewerID = SecurityUtils.requireCurrentUserID();
        return ResponseEntity.ok(reviewService.createReview(reviewerID, req));
    }
}
