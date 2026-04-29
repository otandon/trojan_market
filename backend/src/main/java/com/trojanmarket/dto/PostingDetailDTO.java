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
public class PostingDetailDTO {

    private Integer postID;
    private String title;
    private String description;
    private String category;
    private String status;
    private BigDecimal price;
    private LocalDateTime postTime;
    private Integer sellerID;
    private String sellerUsername;
    private Double sellerRating;
    private String photo;
}
