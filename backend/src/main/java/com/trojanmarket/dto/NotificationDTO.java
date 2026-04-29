package com.trojanmarket.dto;

import com.trojanmarket.entity.NotificationType;
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
public class NotificationDTO {

    private Integer notificationID;
    private Integer receiverID;
    private NotificationType type;
    private Integer relatedPostID;
    private Integer relatedSessionID;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
