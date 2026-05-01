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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final PostingRepository postingRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(ChatSessionRepository sessionRepository,
                       MessageRepository messageRepository,
                       PostingRepository postingRepository,
                       UserRepository userRepository,
                       AuthService authService,
                       NotificationService notificationService,
                       SimpMessagingTemplate messagingTemplate) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.postingRepository = postingRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public ChatSessionDTO getOrCreateSession(Integer postID, Integer buyerID) {
        if (buyerID == null) {
            throw new ForbiddenException("Guests cannot start a chat");
        }
        if (authService.isBannedUser(buyerID)) {
            throw new ForbiddenException("High-risk users cannot message sellers");
        }

        return sessionRepository.findByPostIDAndBuyerID(postID, buyerID)
                .map(this::enrichSessionDTO)
                .orElseGet(() -> {
                    Posting posting = postingRepository.findById(postID)
                            .orElseThrow(() -> new EntityNotFoundException("Posting not found: " + postID));
                    if (posting.getSellerID().equals(buyerID)) {
                        throw new ForbiddenException("Cannot start a chat with yourself");
                    }
                    ChatSession session = ChatSession.builder()
                            .postID(postID)
                            .buyerID(buyerID)
                            .sellerID(posting.getSellerID())
                            .build();
                    return enrichSessionDTO(sessionRepository.save(session));
                });
    }

    public List<ChatSessionDTO> getMySessions(Integer userID) {
        List<ChatSession> sessions = sessionRepository.findByBuyerIDOrSellerID(userID, userID);
        java.util.Set<Integer> postIDs = sessions.stream()
                .map(ChatSession::getPostID)
                .collect(java.util.stream.Collectors.toSet());
        java.util.Set<Integer> userIDs = new java.util.HashSet<>();
        for (ChatSession s : sessions) {
            userIDs.add(s.getBuyerID());
            userIDs.add(s.getSellerID());
        }
        java.util.Map<Integer, Posting> postingsByID = postingRepository.findAllById(postIDs).stream()
                .collect(java.util.stream.Collectors.toMap(Posting::getPostID, p -> p));
        java.util.Map<Integer, User> usersByID = userRepository.findAllById(userIDs).stream()
                .collect(java.util.stream.Collectors.toMap(User::getUserID, u -> u));
        return sessions.stream()
                .map(s -> toSessionDTO(s,
                        postingsByID.get(s.getPostID()),
                        usersByID.get(s.getBuyerID()),
                        usersByID.get(s.getSellerID())))
                .toList();
    }

    private ChatSessionDTO enrichSessionDTO(ChatSession s) {
        Posting posting = postingRepository.findById(s.getPostID()).orElse(null);
        User buyer = userRepository.findById(s.getBuyerID()).orElse(null);
        User seller = userRepository.findById(s.getSellerID()).orElse(null);
        return toSessionDTO(s, posting, buyer, seller);
    }

    @Transactional
    public MessageDTO sendMessage(Integer sessionID, Integer senderID, String text) {
        ChatSession session = validateParticipant(sessionID, senderID);
        if (authService.isBannedUser(senderID)) {
            throw new ForbiddenException("High-risk users cannot send messages");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Message text is required");
        }

        Message saved = messageRepository.save(Message.builder()
                .sessionID(sessionID)
                .senderID(senderID)
                .messageText(text)
                .isRead(false)
                .build());

        MessageDTO dto = toMessageDTO(saved);

        // Broadcast to both participants subscribed to /topic/chat/{sessionID}.
        messagingTemplate.convertAndSend("/topic/chat/" + sessionID, dto);

        Integer recipientID = session.getBuyerID().equals(senderID)
                ? session.getSellerID()
                : session.getBuyerID();
        notificationService.createNotification(
                recipientID, NotificationType.NEW_MESSAGE, session.getPostID(), sessionID);

        return dto;
    }

    public List<MessageDTO> getHistory(Integer sessionID, Integer callerID) {
        validateParticipant(sessionID, callerID);
        return messageRepository.findBySessionIDOrderByMessageTimeAsc(sessionID).stream()
                .map(this::toMessageDTO)
                .toList();
    }

    public ChatSession validateParticipant(Integer sessionID, Integer userID) {
        ChatSession session = sessionRepository.findById(sessionID)
                .orElseThrow(() -> new EntityNotFoundException("Chat session not found: " + sessionID));
        if (!session.getBuyerID().equals(userID) && !session.getSellerID().equals(userID)) {
            throw new ForbiddenException("Not a participant in this chat session");
        }
        return session;
    }

    private ChatSessionDTO toSessionDTO(ChatSession s, Posting posting, User buyer, User seller) {
        return ChatSessionDTO.builder()
                .sessionID(s.getSessionID())
                .postID(s.getPostID())
                .postTitle(posting == null ? null : posting.getTitle())
                .buyerID(s.getBuyerID())
                .buyerFirstName(buyer == null ? null : buyer.getFirstName())
                .buyerLastName(buyer == null ? null : buyer.getLastName())
                .sellerID(s.getSellerID())
                .sellerFirstName(seller == null ? null : seller.getFirstName())
                .sellerLastName(seller == null ? null : seller.getLastName())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private MessageDTO toMessageDTO(Message m) {
        return MessageDTO.builder()
                .messageID(m.getMessageID())
                .sessionID(m.getSessionID())
                .senderID(m.getSenderID())
                .messageText(m.getMessageText())
                .messageTime(m.getMessageTime())
                .isRead(m.getIsRead())
                .build();
    }
}
