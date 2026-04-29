package com.trojanmarket.controller;

import com.trojanmarket.dto.MessageDTO;
import com.trojanmarket.dto.SavedPostingDTO;
import com.trojanmarket.dto.TransactionDTO;
import com.trojanmarket.manager.StatsManager;
import com.trojanmarket.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stats")
public class StatsController {

    private final StatsManager statsManager;

    public StatsController(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    @GetMapping("/past-purchases")
    public ResponseEntity<List<TransactionDTO>> pastPurchases() {
        Integer userID = SecurityUtils.requireCurrentUserID();
        return ResponseEntity.ok(statsManager.getPastPurchases(userID));
    }

    @GetMapping("/sold-items")
    public ResponseEntity<List<TransactionDTO>> soldItems() {
        Integer userID = SecurityUtils.requireCurrentUserID();
        return ResponseEntity.ok(statsManager.getSoldItems(userID));
    }

    @GetMapping("/saved-postings")
    public ResponseEntity<List<SavedPostingDTO>> savedPostings() {
        Integer userID = SecurityUtils.requireCurrentUserID();
        return ResponseEntity.ok(statsManager.getSavedPostings(userID));
    }

    @GetMapping("/chat/{postID}")
    public ResponseEntity<List<MessageDTO>> chatForPost(@PathVariable Integer postID) {
        Integer userID = SecurityUtils.requireCurrentUserID();
        return ResponseEntity.ok(statsManager.getChat(postID, userID));
    }

    @DeleteMapping("/saved-postings/{postID}")
    public ResponseEntity<Void> removeSavedPosting(@PathVariable Integer postID) {
        Integer userID = SecurityUtils.requireCurrentUserID();
        statsManager.removeSavedPosting(userID, postID);
        return ResponseEntity.noContent().build();
    }
}
