package com.trojanmarket.controller;

import com.trojanmarket.dto.ChatMessageDTO;
import com.trojanmarket.dto.ChatSessionDTO;
import com.trojanmarket.dto.MessageDTO;
import com.trojanmarket.security.AuthenticatedUser;
import com.trojanmarket.security.ForbiddenException;
import com.trojanmarket.security.SecurityUtils;
import com.trojanmarket.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionDTO> getOrCreateSession(@RequestBody Map<String, Integer> body) {
        Integer postID = body.get("postID");
        if (postID == null) {
            throw new IllegalArgumentException("postID is required");
        }
        Integer buyerID = SecurityUtils.requireCurrentUserID();
        return ResponseEntity.ok(chatService.getOrCreateSession(postID, buyerID));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionDTO>> getMySessions() {
        Integer userID = SecurityUtils.requireCurrentUserID();
        return ResponseEntity.ok(chatService.getMySessions(userID));
    }

    @GetMapping("/sessions/{sessionID}/messages")
    public ResponseEntity<List<MessageDTO>> getHistory(@PathVariable Integer sessionID) {
        Integer userID = SecurityUtils.requireCurrentUserID();
        return ResponseEntity.ok(chatService.getHistory(sessionID, userID));
    }

    @MessageMapping("/chat.send")
    public void handleMessage(ChatMessageDTO message, Principal principal) {
        if (!(principal instanceof AuthenticatedUser user)) {
            throw new ForbiddenException("Not authenticated");
        }
        // CRITICAL: senderID is ALWAYS extracted from the authenticated principal,
        // never from the client payload. Anything in message.getSenderID() is ignored.
        chatService.sendMessage(message.getSessionID(), user.getUserID(), message.getMessageText());
    }
}
