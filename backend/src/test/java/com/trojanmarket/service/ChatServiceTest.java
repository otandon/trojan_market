package com.trojanmarket.service;

import com.trojanmarket.dto.ChatSessionDTO;
import com.trojanmarket.dto.MessageDTO;
import com.trojanmarket.entity.ChatSession;
import com.trojanmarket.entity.Message;
import com.trojanmarket.entity.NotificationType;
import com.trojanmarket.entity.Posting;
import com.trojanmarket.entity.User;
import com.trojanmarket.repository.ChatSessionRepository;
import com.trojanmarket.repository.MessageRepository;
import com.trojanmarket.repository.PostingRepository;
import com.trojanmarket.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ChatService}.
 *
 * Covers the test plan's "Messaging System" unit + white-box + black-box sections:
 *   - getOrCreateSession: new vs existing, self-chat refused, high-risk buyer blocked
 *   - sendMessage: persists + broadcasts + triggers notification, server-side senderID,
 *                  empty text refused, banned user blocked
 *   - getHistory: chronological, empty list for new session, outsider refused
 *   - validateParticipant: passes for buyer/seller, throws ForbiddenException for outsiders
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock ChatSessionRepository sessionRepository;
    @Mock MessageRepository messageRepository;
    @Mock PostingRepository postingRepository;
    @Mock UserRepository userRepository;
    @Mock AuthService authService;
    @Mock NotificationService notificationService;
    @Mock SimpMessagingTemplate messagingTemplate;
    @InjectMocks ChatService chatService;

    private static Posting posting(int postID, int sellerID, String title) {
        return Posting.builder().postID(postID).sellerID(sellerID).title(title).build();
    }

    private static ChatSession session(int sessionID, int postID, int buyerID, int sellerID) {
        return ChatSession.builder()
                .sessionID(sessionID).postID(postID).buyerID(buyerID).sellerID(sellerID).build();
    }

    @Nested
    @DisplayName("getOrCreateSession")
    class GetOrCreate {

        @Test
        void createsNewSessionWhenNoneExists() {
            when(authService.isBannedUser(2)).thenReturn(false);
            when(sessionRepository.findByPostIDAndBuyerID(10, 2)).thenReturn(Optional.empty());
            when(postingRepository.findById(10)).thenReturn(Optional.of(posting(10, 1, "Camera")));
            when(sessionRepository.save(any(ChatSession.class))).thenAnswer(inv -> {
                ChatSession s = inv.getArgument(0);
                s.setSessionID(99);
                return s;
            });
            // No need to look up users individually for the new-session path —
            // the service uses the posting it already loaded for the title.
            when(userRepository.findById(anyInt())).thenReturn(Optional.empty());

            ChatSessionDTO dto = chatService.getOrCreateSession(10, 2);

            ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
            verify(sessionRepository).save(captor.capture());
            assertThat(captor.getValue().getSellerID()).isEqualTo(1);
            assertThat(captor.getValue().getBuyerID()).isEqualTo(2);
            assertThat(captor.getValue().getPostID()).isEqualTo(10);
            assertThat(dto.getSessionID()).isEqualTo(99);
            assertThat(dto.getPostTitle()).isEqualTo("Camera");
        }

        @Test
        void returnsExistingSessionWithoutDuplicating() {
            ChatSession existing = session(50, 10, 2, 1);
            when(authService.isBannedUser(2)).thenReturn(false);
            when(sessionRepository.findByPostIDAndBuyerID(10, 2)).thenReturn(Optional.of(existing));
            when(postingRepository.findById(10)).thenReturn(Optional.of(posting(10, 1, "Camera")));

            ChatSessionDTO dto = chatService.getOrCreateSession(10, 2);

            assertThat(dto.getSessionID()).isEqualTo(50);
            verify(sessionRepository, never()).save(any());
        }

        @Test
        void refusesGuestBuyer() {
            assertThatThrownBy(() -> chatService.getOrCreateSession(10, null))
                    .isInstanceOf(ForbiddenException.class);
            verify(sessionRepository, never()).save(any());
        }

        @Test
        void refusesHighRiskBuyer() {
            when(authService.isBannedUser(2)).thenReturn(true);
            assertThatThrownBy(() -> chatService.getOrCreateSession(10, 2))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("High-risk");
        }

        @Test
        void refusesSelfChat() {
            // Buyer == seller is rejected (you can't chat with yourself on your own listing).
            when(authService.isBannedUser(1)).thenReturn(false);
            when(sessionRepository.findByPostIDAndBuyerID(10, 1)).thenReturn(Optional.empty());
            when(postingRepository.findById(10)).thenReturn(Optional.of(posting(10, 1, "Camera")));

            assertThatThrownBy(() -> chatService.getOrCreateSession(10, 1))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("yourself");
        }

        @Test
        void postingMustExist() {
            when(authService.isBannedUser(2)).thenReturn(false);
            when(sessionRepository.findByPostIDAndBuyerID(99, 2)).thenReturn(Optional.empty());
            when(postingRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.getOrCreateSession(99, 2))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("sendMessage")
    class SendMessage {

        @Test
        void persistsBroadcastsAndNotifiesRecipient() {
            ChatSession s = session(50, 10, 2, 1);
            when(sessionRepository.findById(50)).thenReturn(Optional.of(s));
            when(authService.isBannedUser(2)).thenReturn(false);
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setMessageID(1001);
                m.setMessageTime(LocalDateTime.now());
                m.setIsRead(false);
                return m;
            });

            MessageDTO dto = chatService.sendMessage(50, 2, "hello");

            // 1. Persisted with senderID from the principal (NOT from a payload).
            ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository).save(msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSenderID()).isEqualTo(2);
            assertThat(msgCaptor.getValue().getSessionID()).isEqualTo(50);
            assertThat(msgCaptor.getValue().getMessageText()).isEqualTo("hello");

            // 2. Broadcast to /topic/chat/{sessionID}.
            verify(messagingTemplate).convertAndSend("/topic/chat/50", dto);

            // 3. Recipient (the seller, since buyer sent it) gets a NEW_MESSAGE notification.
            verify(notificationService).createNotification(1, NotificationType.NEW_MESSAGE, 10, 50);
        }

        @Test
        void recipientIsBuyerWhenSellerSends() {
            ChatSession s = session(50, 10, 2, 1);
            when(sessionRepository.findById(50)).thenReturn(Optional.of(s));
            when(authService.isBannedUser(1)).thenReturn(false);
            when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m.setMessageID(1002);
                return m;
            });

            chatService.sendMessage(50, 1, "yo");

            verify(notificationService).createNotification(2, NotificationType.NEW_MESSAGE, 10, 50);
        }

        @Test
        void emptyTextRejected() {
            ChatSession s = session(50, 10, 2, 1);
            when(sessionRepository.findById(50)).thenReturn(Optional.of(s));
            when(authService.isBannedUser(2)).thenReturn(false);

            assertThatThrownBy(() -> chatService.sendMessage(50, 2, "   "))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(messageRepository, never()).save(any());
            verify(messagingTemplate, never()).convertAndSend(anyString(), (Object) any());
        }

        @Test
        void bannedSenderBlocked() {
            ChatSession s = session(50, 10, 2, 1);
            when(sessionRepository.findById(50)).thenReturn(Optional.of(s));
            when(authService.isBannedUser(2)).thenReturn(true);

            assertThatThrownBy(() -> chatService.sendMessage(50, 2, "hello"))
                    .isInstanceOf(ForbiddenException.class);
            verify(messageRepository, never()).save(any());
        }

        @Test
        void outsiderCannotSend() {
            ChatSession s = session(50, 10, 2, 1);
            when(sessionRepository.findById(50)).thenReturn(Optional.of(s));

            assertThatThrownBy(() -> chatService.sendMessage(50, 999, "hi"))
                    .isInstanceOf(ForbiddenException.class);
            verify(messageRepository, never()).save(any());
        }

        @Test
        void nonExistentSessionThrows() {
            when(sessionRepository.findById(123)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.sendMessage(123, 2, "hello"))
                    .isInstanceOf(EntityNotFoundException.class);
            verify(messageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("validateParticipant")
    class Validate {

        @Test
        void passesForBuyer() {
            when(sessionRepository.findById(50)).thenReturn(Optional.of(session(50, 10, 2, 1)));
            ChatSession returned = chatService.validateParticipant(50, 2);
            assertThat(returned.getSessionID()).isEqualTo(50);
        }

        @Test
        void passesForSeller() {
            when(sessionRepository.findById(50)).thenReturn(Optional.of(session(50, 10, 2, 1)));
            ChatSession returned = chatService.validateParticipant(50, 1);
            assertThat(returned.getSessionID()).isEqualTo(50);
        }

        @Test
        void rejectsOutsider() {
            when(sessionRepository.findById(50)).thenReturn(Optional.of(session(50, 10, 2, 1)));
            assertThatThrownBy(() -> chatService.validateParticipant(50, 999))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("getHistory")
    class History {

        @Test
        void returnsMessagesInChronologicalOrder() {
            when(sessionRepository.findById(50)).thenReturn(Optional.of(session(50, 10, 2, 1)));
            Message m1 = Message.builder().messageID(1).sessionID(50).senderID(2).messageText("hi")
                    .messageTime(LocalDateTime.now().minusMinutes(2)).isRead(false).build();
            Message m2 = Message.builder().messageID(2).sessionID(50).senderID(1).messageText("hello")
                    .messageTime(LocalDateTime.now().minusMinutes(1)).isRead(false).build();
            when(messageRepository.findBySessionIDOrderByMessageTimeAsc(50)).thenReturn(List.of(m1, m2));

            List<MessageDTO> out = chatService.getHistory(50, 2);

            assertThat(out).hasSize(2);
            assertThat(out.get(0).getMessageID()).isEqualTo(1);
            assertThat(out.get(1).getMessageID()).isEqualTo(2);
        }

        @Test
        void newSessionReturnsEmptyList() {
            when(sessionRepository.findById(50)).thenReturn(Optional.of(session(50, 10, 2, 1)));
            when(messageRepository.findBySessionIDOrderByMessageTimeAsc(50)).thenReturn(List.of());
            assertThat(chatService.getHistory(50, 2)).isEmpty();
        }

        @Test
        void outsiderCannotReadHistory() {
            when(sessionRepository.findById(50)).thenReturn(Optional.of(session(50, 10, 2, 1)));
            assertThatThrownBy(() -> chatService.getHistory(50, 999))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("getMySessions")
    class MySessions {

        @Test
        void enrichesWithBuyerAndSellerNames() {
            ChatSession s = session(50, 10, 2, 1);
            when(sessionRepository.findByBuyerIDOrSellerID(2, 2)).thenReturn(List.of(s));
            when(postingRepository.findAllById(any())).thenReturn(List.of(posting(10, 1, "Camera")));
            when(userRepository.findAllById(any())).thenReturn(List.of(
                    User.builder().userID(1).firstName("Sam").lastName("Seller").build(),
                    User.builder().userID(2).firstName("Bea").lastName("Buyer").build()
            ));

            List<ChatSessionDTO> dtos = chatService.getMySessions(2);

            assertThat(dtos).hasSize(1);
            ChatSessionDTO d = dtos.get(0);
            assertThat(d.getPostTitle()).isEqualTo("Camera");
            assertThat(d.getBuyerFirstName()).isEqualTo("Bea");
            assertThat(d.getSellerFirstName()).isEqualTo("Sam");
        }
    }
}
