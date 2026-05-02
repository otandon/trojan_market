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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NotificationService}.
 *
 * Maps to the test plan's "Notifications System" section:
 *   - createNotification respects preferences (skips when disabled)
 *   - createNotification persists + broadcasts when enabled, with default-true semantics
 *   - getNotifications returns most-recent-first (delegated to repository ordering)
 *   - getUnreadCount returns the COUNT of isRead=false rows
 *   - markRead sets isRead=true; rejects another user marking it
 *   - clearAll removes all rows for the user
 *   - updatePreferences persists, partial-update friendly
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationPreferencesRepository preferencesRepository;
    @Mock SimpMessagingTemplate messagingTemplate;
    @InjectMocks NotificationService service;

    @Nested
    @DisplayName("createNotification")
    class Create {

        @Test
        void persistsAndBroadcastsWhenPreferenceAllows() {
            // No prefs row → defaults are all-on per the schema DEFAULT TRUE.
            when(preferencesRepository.findById(2)).thenReturn(Optional.empty());
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setNotificationID(99);
                n.setCreatedAt(LocalDateTime.now());
                return n;
            });
            when(notificationRepository.countByReceiverIDAndIsReadFalse(2)).thenReturn(1L);

            Optional<NotificationDTO> dto = service.createNotification(
                    2, NotificationType.NEW_MESSAGE, 10, 50);

            assertThat(dto).isPresent();
            verify(notificationRepository).save(any(Notification.class));
            verify(messagingTemplate).convertAndSendToUser("2", "/queue/notifications", dto.get());
            verify(messagingTemplate).convertAndSendToUser("2", "/queue/unread", 1L);
        }

        @Test
        void skipsWhenPreferenceDisabled() {
            NotificationPreferences prefs = NotificationPreferences.builder()
                    .userID(2).newMessage(false).newOffer(true).offerResponse(true).itemSold(true)
                    .build();
            when(preferencesRepository.findById(2)).thenReturn(Optional.of(prefs));

            Optional<NotificationDTO> dto = service.createNotification(
                    2, NotificationType.NEW_MESSAGE, 10, 50);

            assertThat(dto).isEmpty();
            verify(notificationRepository, never()).save(any());
            verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any(Object.class));
        }

        @Test
        void itemSoldUsesItemSoldPreference() {
            NotificationPreferences prefs = NotificationPreferences.builder()
                    .userID(2).newMessage(true).newOffer(true).offerResponse(true).itemSold(false)
                    .build();
            when(preferencesRepository.findById(2)).thenReturn(Optional.of(prefs));

            Optional<NotificationDTO> dto = service.createNotification(
                    2, NotificationType.ITEM_SOLD, 10, null);

            assertThat(dto).isEmpty();
        }

        @Test
        void offerAcceptedUsesOfferResponsePreference() {
            NotificationPreferences prefs = NotificationPreferences.builder()
                    .userID(2).newMessage(true).newOffer(true).offerResponse(false).itemSold(true)
                    .build();
            when(preferencesRepository.findById(2)).thenReturn(Optional.of(prefs));

            assertThat(service.createNotification(2, NotificationType.OFFER_ACCEPTED, 10, null))
                    .isEmpty();
            assertThat(service.createNotification(2, NotificationType.OFFER_REJECTED, 10, null))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("read state")
    class ReadState {

        @Test
        void markReadFlipsFlagAndPushesUpdatedCount() {
            Notification n = Notification.builder().notificationID(7).receiverID(2)
                    .type(NotificationType.NEW_MESSAGE).isRead(false).build();
            when(notificationRepository.findById(7)).thenReturn(Optional.of(n));
            when(notificationRepository.countByReceiverIDAndIsReadFalse(2)).thenReturn(0L);

            service.markRead(7, 2);

            assertThat(n.getIsRead()).isTrue();
            verify(notificationRepository).save(n);
            verify(messagingTemplate).convertAndSendToUser("2", "/queue/unread", 0L);
        }

        @Test
        void markReadRejectsForeignNotification() {
            Notification n = Notification.builder().notificationID(7).receiverID(2)
                    .type(NotificationType.NEW_MESSAGE).isRead(false).build();
            when(notificationRepository.findById(7)).thenReturn(Optional.of(n));

            assertThatThrownBy(() -> service.markRead(7, 999))
                    .isInstanceOf(ForbiddenException.class);
            verify(notificationRepository, never()).save(any());
        }

        @Test
        void markReadOnUnknownThrows() {
            when(notificationRepository.findById(7)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.markRead(7, 2))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        void clearAllDeletesAndZeroesUnreadBadge() {
            service.clearAll(2);
            verify(notificationRepository).deleteByReceiverID(2);
            verify(messagingTemplate).convertAndSendToUser("2", "/queue/unread", 0L);
        }
    }

    @Nested
    @DisplayName("getUnreadCount + getNotifications")
    class Reads {

        @Test
        void unreadCountDelegatesToRepository() {
            when(notificationRepository.countByReceiverIDAndIsReadFalse(2)).thenReturn(3L);
            assertThat(service.getUnreadCount(2)).isEqualTo(3L);
        }

        @Test
        void getNotificationsReturnsRepositoryOrderingMostRecentFirst() {
            // Repository method already orders by createdAt DESC; we just ensure pass-through.
            Notification newer = Notification.builder().notificationID(2).receiverID(2)
                    .type(NotificationType.NEW_MESSAGE).isRead(false)
                    .createdAt(LocalDateTime.now()).build();
            Notification older = Notification.builder().notificationID(1).receiverID(2)
                    .type(NotificationType.NEW_MESSAGE).isRead(true)
                    .createdAt(LocalDateTime.now().minusHours(1)).build();
            when(notificationRepository.findByReceiverIDOrderByCreatedAtDesc(2))
                    .thenReturn(List.of(newer, older));

            List<NotificationDTO> out = service.getNotifications(2);
            assertThat(out).extracting(NotificationDTO::getNotificationID).containsExactly(2, 1);
        }
    }

    @Nested
    @DisplayName("preferences")
    class Preferences {

        @Test
        void updatePersistsAndReturnsCurrentSettings() {
            NotificationPreferences existing = NotificationPreferences.builder()
                    .userID(2).newMessage(true).newOffer(true).offerResponse(true).itemSold(true)
                    .build();
            when(preferencesRepository.findById(2)).thenReturn(Optional.of(existing));
            when(preferencesRepository.save(any(NotificationPreferences.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            NotificationPreferencesDTO incoming = NotificationPreferencesDTO.builder()
                    .newMessage(false).build();
            NotificationPreferencesDTO out = service.updatePreferences(2, incoming);

            ArgumentCaptor<NotificationPreferences> captor =
                    ArgumentCaptor.forClass(NotificationPreferences.class);
            verify(preferencesRepository).save(captor.capture());
            assertThat(captor.getValue().getNewMessage()).isFalse();
            // Untouched fields preserved.
            assertThat(captor.getValue().getNewOffer()).isTrue();
            assertThat(captor.getValue().getOfferResponse()).isTrue();
            assertThat(captor.getValue().getItemSold()).isTrue();
            assertThat(out.getNewMessage()).isFalse();
        }

        @Test
        void updateCreatesRowWhenAbsent() {
            when(preferencesRepository.findById(2)).thenReturn(Optional.empty());
            when(preferencesRepository.save(any(NotificationPreferences.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            NotificationPreferencesDTO incoming = NotificationPreferencesDTO.builder()
                    .newMessage(false).build();
            service.updatePreferences(2, incoming);

            ArgumentCaptor<NotificationPreferences> captor =
                    ArgumentCaptor.forClass(NotificationPreferences.class);
            verify(preferencesRepository).save(captor.capture());
            assertThat(captor.getValue().getUserID()).isEqualTo(2);
            assertThat(captor.getValue().getNewMessage()).isFalse();
        }

        @Test
        void getReturnsDefaultsWhenNoRow() {
            when(preferencesRepository.findById(2)).thenReturn(Optional.empty());
            NotificationPreferencesDTO out = service.getPreferences(2);
            assertThat(out.getNewMessage()).isTrue();
            assertThat(out.getNewOffer()).isTrue();
            assertThat(out.getOfferResponse()).isTrue();
            assertThat(out.getItemSold()).isTrue();
        }
    }
}
