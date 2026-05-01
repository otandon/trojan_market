package com.trojanmarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDTO {

    private Integer transactionID;
    private Integer postID;
    private String postTitle;
    private Integer buyerID;
    private String buyerUsername;
    private Integer sellerID;
    private String sellerUsername;
    private BigDecimal salePrice;
    private LocalDateTime transactionTime;
    /** Set if the buyer has already reviewed this transaction. Null otherwise. */
    private Integer reviewID;
}
