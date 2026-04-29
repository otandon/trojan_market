package com.trojanmarket.controller;

import com.trojanmarket.dto.CreatePostingRequest;
import com.trojanmarket.dto.EditPostingRequest;
import com.trojanmarket.entity.Posting;
import com.trojanmarket.security.SecurityUtils;
import com.trojanmarket.service.PostingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/postings")
public class PostingController {

    private final PostingService postingService;

    public PostingController(PostingService postingService) {
        this.postingService = postingService;
    }

    @PostMapping
    public ResponseEntity<Posting> create(@Valid @RequestBody CreatePostingRequest req) {
        Integer sellerID = SecurityUtils.requireCurrentUserID();
        return ResponseEntity.ok(postingService.createPosting(sellerID, req));
    }

    @PutMapping("/{postID}")
    public ResponseEntity<Posting> edit(@PathVariable Integer postID,
                                        @RequestBody EditPostingRequest req) {
        Integer callerID = SecurityUtils.requireCurrentUserID();
        return ResponseEntity.ok(postingService.editPosting(postID, callerID, req));
    }

    @DeleteMapping("/{postID}")
    public ResponseEntity<Void> softDelete(@PathVariable Integer postID) {
        Integer callerID = SecurityUtils.requireCurrentUserID();
        postingService.softDelete(postID, callerID);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mine")
    public ResponseEntity<List<Posting>> myPostings() {
        Integer sellerID = SecurityUtils.requireCurrentUserID();
        return ResponseEntity.ok(postingService.getActiveByseller(sellerID));
    }
}
