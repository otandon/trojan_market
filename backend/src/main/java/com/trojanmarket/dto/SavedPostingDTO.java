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
public class SavedPostingDTO {

    private Integer savedID;
    private Integer postID;
    private String title;
    private BigDecimal price;
    private String status;
    private Integer sellerID;
    private String sellerUsername;
    private Double sellerRating;
    private LocalDateTime savedTime;
}
