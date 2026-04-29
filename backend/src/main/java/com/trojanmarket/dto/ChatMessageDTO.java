package com.trojanmarket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {

    private Integer sessionID;

    // senderID is intentionally NOT trusted from client — server populates from JWT.
    private Integer senderID;

    private String messageText;
}
