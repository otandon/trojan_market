package com.trojanmarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferencesDTO {

    private Boolean newMessage;
    private Boolean newOffer;
    private Boolean offerResponse;
    private Boolean itemSold;
}
