package com.trojanmarket.dto;

import com.trojanmarket.entity.Category;
import com.trojanmarket.entity.PostingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EditPostingRequest {

    private String title;
    private String description;
    private Category category;
    private BigDecimal price;
    private PostingStatus status;

    // Required when status transitions to SOLD — identifies the buyer for the Transaction.
    private Integer buyerID;
    private BigDecimal salePrice;
}
