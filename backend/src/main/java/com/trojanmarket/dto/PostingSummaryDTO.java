package com.trojanmarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostingSummaryDTO {

    private Integer postID;
    private String title;
    private BigDecimal price;
    private String photo;
    private Integer sellerID;
    private Double sellerRating;
    private String status;
}
