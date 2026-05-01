package com.trojanmarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDTO {

    private Integer reviewID;
    private Integer reviewerID;
    private String reviewerFirstName;
    private String reviewerLastName;
    private Integer sellerID;
    private Integer transactionID;
    private Integer postID;
    private String postTitle;
    private Short rating;
    private String comment;
    private LocalDateTime reviewTime;
}
