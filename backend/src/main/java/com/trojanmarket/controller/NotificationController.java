package com.trojanmarket.controller;

import com.trojanmarket.dto.NotificationDTO;
import com.trojanmarket.dto.NotificationPreferencesDTO;
import com.trojanmarket.security.SecurityUtils;
import com.trojanmarket.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> list() {
        Integer userID = SecurityUtils.requireCurrentUserID();
        return ResponseEntity.ok(notificationService.getNotifications(userID));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        Integer userID = SecurityUtils.requireCurrentUserID();
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(userID)));
    }

    @PutMapping("/{notificationID}/read")
    public ResponseEntity<Void> markRead(@PathVariable Integer notificationID) {
        Integer userID = SecurityUtils.requireCurrentUserID();
        notificationService.markRead(notificationID, userID);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearAll() {
        Integer userID = SecurityUtils.requireCurrentUserID();
        notificationService.clearAll(userID);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferencesDTO> getPreferences() {
        Integer userID = SecurityUtils.requireCurrentUserID();
        return ResponseEntity.ok(notificationService.getPreferences(userID));
    }

    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferencesDTO> updatePreferences(@RequestBody NotificationPreferencesDTO body) {
        Integer userID = SecurityUtils.requireCurrentUserID();
        return ResponseEntity.ok(notificationService.updatePreferences(userID, body));
    }
}
