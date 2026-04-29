package com.trojanmarket.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "messageID")
    private Integer messageID;

    @Column(name = "sessionID", nullable = false)
    private Integer sessionID;

    @Column(name = "senderID", nullable = false)
    private Integer senderID;

    @Column(name = "messageText", nullable = false, columnDefinition = "TEXT")
    private String messageText;

    @CreationTimestamp
    @Column(name = "messageTime", nullable = false, updatable = false)
    private LocalDateTime messageTime;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = Boolean.FALSE;
}
