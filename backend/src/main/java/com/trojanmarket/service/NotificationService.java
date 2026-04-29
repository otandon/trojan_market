package com.trojanmarket.service;

import com.trojanmarket.dto.NotificationDTO;
import com.trojanmarket.dto.NotificationPreferencesDTO;
import com.trojanmarket.entity.Notification;
import com.trojanmarket.entity.NotificationPreferences;
import com.trojanmarket.entity.NotificationType;
import com.trojanmarket.repository.NotificationPreferencesRepository;
import com.trojanmarket.repository.NotificationRepository;
import com.trojanmarket.security.ForbiddenException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferencesRepository preferencesRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationPreferencesRepository preferencesRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.preferencesRepository = preferencesRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Optional<NotificationDTO> createNotification(Integer receiverID,
                                                       NotificationType type,
                                                       Integer relatedPostID,
                                                       Integer relatedSessionID) {
        if (!isPreferenceEnabled(receiverID, type)) {
            return Optional.empty();
        }

        Notification saved = notificationRepository.save(Notification.builder()
                .receiverID(receiverID)
                .type(type)
                .relatedPostID(relatedPostID)
                .relatedSessionID(relatedSessionID)
                .isRead(false)
                .build());

        NotificationDTO dto = toDTO(saved);
        String userKey = String.valueOf(receiverID);
        messagingTemplate.convertAndSendToUser(userKey, "/queue/notifications", dto);
        messagingTemplate.convertAndSendToUser(userKey, "/queue/unread", getUnreadCount(receiverID));
        return Optional.of(dto);
    }

    public List<NotificationDTO> getNotifications(Integer userID) {
        return notificationRepository.findByReceiverIDOrderByCreatedAtDesc(userID).stream()
                .map(this::toDTO)
                .toList();
    }

    public long getUnreadCount(Integer userID) {
        return notificationRepository.countByReceiverIDAndIsReadFalse(userID);
    }

    @Transactional
    public void markRead(Integer notificationID, Integer callerID) {
        Notification n = notificationRepository.findById(notificationID)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: " + notificationID));
        if (!n.getReceiverID().equals(callerID)) {
            throw new ForbiddenException("Cannot mark another user's notification as read");
        }
        n.setIsRead(true);
        notificationRepository.save(n);
        messagingTemplate.convertAndSendToUser(String.valueOf(callerID), "/queue/unread", getUnreadCount(callerID));
    }

    @Transactional
    public void clearAll(Integer userID) {
        notificationRepository.deleteByReceiverID(userID);
        messagingTemplate.convertAndSendToUser(String.valueOf(userID), "/queue/unread", 0L);
    }

    @Transactional
    public NotificationPreferencesDTO updatePreferences(Integer userID, NotificationPreferencesDTO incoming) {
        NotificationPreferences prefs = preferencesRepository.findById(userID)
                .orElseGet(() -> NotificationPreferences.builder()
                        .userID(userID)
                        .newMessage(true)
                        .newOffer(true)
                        .offerResponse(true)
                        .itemSold(true)
                        .build());

        if (incoming.getNewMessage()    != null) prefs.setNewMessage(incoming.getNewMessage());
        if (incoming.getNewOffer()      != null) prefs.setNewOffer(incoming.getNewOffer());
        if (incoming.getOfferResponse() != null) prefs.setOfferResponse(incoming.getOfferResponse());
        if (incoming.getItemSold()      != null) prefs.setItemSold(incoming.getItemSold());

        NotificationPreferences saved = preferencesRepository.save(prefs);
        return toPreferencesDTO(saved);
    }

    public NotificationPreferencesDTO getPreferences(Integer userID) {
        return preferencesRepository.findById(userID)
                .map(this::toPreferencesDTO)
                .orElse(NotificationPreferencesDTO.builder()
                        .newMessage(true).newOffer(true).offerResponse(true).itemSold(true)
                        .build());
    }

    // --- internals ----------------------------------------------------------

    private boolean isPreferenceEnabled(Integer userID, NotificationType type) {
        NotificationPreferences prefs = preferencesRepository.findById(userID).orElse(null);
        if (prefs == null) {
            // No row yet — defaults are all-on per the schema DEFAULT TRUE.
            return true;
        }
        return switch (type) {
            case NEW_MESSAGE -> Boolean.TRUE.equals(prefs.getNewMessage());
            case NEW_OFFER -> Boolean.TRUE.equals(prefs.getNewOffer());
            case OFFER_ACCEPTED, OFFER_REJECTED -> Boolean.TRUE.equals(prefs.getOfferResponse());
            case ITEM_SOLD, ITEM_PURCHASED -> Boolean.TRUE.equals(prefs.getItemSold());
        };
    }

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .notificationID(n.getNotificationID())
                .receiverID(n.getReceiverID())
                .type(n.getType())
                .relatedPostID(n.getRelatedPostID())
                .relatedSessionID(n.getRelatedSessionID())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private NotificationPreferencesDTO toPreferencesDTO(NotificationPreferences p) {
        return NotificationPreferencesDTO.builder()
                .newMessage(p.getNewMessage())
                .newOffer(p.getNewOffer())
                .offerResponse(p.getOfferResponse())
                .itemSold(p.getItemSold())
                .build();
    }
}
