package com.trojanmarket.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "NotificationPreferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferences {

    @Id
    @Column(name = "userID")
    private Integer userID;

    @Column(name = "new_message", nullable = false)
    private Boolean newMessage = Boolean.TRUE;

    @Column(name = "new_offer", nullable = false)
    private Boolean newOffer = Boolean.TRUE;

    @Column(name = "offer_response", nullable = false)
    private Boolean offerResponse = Boolean.TRUE;

    @Column(name = "item_sold", nullable = false)
    private Boolean itemSold = Boolean.TRUE;
}
