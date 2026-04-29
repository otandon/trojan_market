package com.trojanmarket.controller;

import com.trojanmarket.dto.PostingDetailDTO;
import com.trojanmarket.dto.PostingSummaryDTO;
import com.trojanmarket.manager.SearchManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final SearchManager searchManager;

    public SearchController(SearchManager searchManager) {
        this.searchManager = searchManager;
    }

    @GetMapping
    public ResponseEntity<List<PostingSummaryDTO>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "relevant") String sortBy) {
        return ResponseEntity.ok(searchManager.searchPostings(q, category, minPrice, maxPrice, sortBy));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> categories() {
        return ResponseEntity.ok(searchManager.getCategories());
    }

    @GetMapping("/postings/{postID}")
    public ResponseEntity<PostingDetailDTO> postingDetail(@PathVariable Integer postID) {
        return ResponseEntity.ok(searchManager.getPostingDetail(postID));
    }
}
