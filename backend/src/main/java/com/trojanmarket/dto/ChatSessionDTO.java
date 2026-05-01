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
public class ChatSessionDTO {

    private Integer sessionID;
    private Integer postID;
    private String postTitle;
    private Integer buyerID;
    private String buyerFirstName;
    private String buyerLastName;
    private Integer sellerID;
    private String sellerFirstName;
    private String sellerLastName;
    private LocalDateTime createdAt;
}
