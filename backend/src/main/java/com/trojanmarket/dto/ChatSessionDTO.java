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
    private Integer sellerID;
    private LocalDateTime createdAt;
}
