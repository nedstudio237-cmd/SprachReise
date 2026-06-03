package com.sprachreise.api.controller;

import com.sprachreise.api.entity.Notification;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.NotificationRepository;
import com.sprachreise.api.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository         userRepository;

    public NotificationController(NotificationRepository notificationRepository,
                                   UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository         = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> getMyNotifications(Authentication auth) {
        User me = currentUser(auth);
        if (me == null) return ResponseEntity.status(401).build();

        List<Map<String,Object>> result = notificationRepository
            .findByUserIdOrderByCreatedAtDesc(me.getId())
            .stream()
            .map(n -> {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("id",        n.getId());
                m.put("type",      n.getType());
                m.put("title",     n.getTitle());
                m.put("body",      n.getBody());
                m.put("entityId",  n.getEntityId());
                m.put("isRead",    n.getIsRead());
                m.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
                return m;
            }).toList();

        long unread = notificationRepository.countUnread(me.getId());
        return ResponseEntity.ok(Map.of("notifications", result, "unreadCount", unread));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markRead(Authentication auth, @PathVariable Long id) {
        User me = currentUser(auth);
        if (me == null) return ResponseEntity.status(401).build();
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getUserId().equals(me.getId())) {
                n.setIsRead(true);
                notificationRepository.save(n);
            }
        });
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/read-all")
    @Transactional
    public ResponseEntity<?> markAllRead(Authentication auth) {
        User me = currentUser(auth);
        if (me == null) return ResponseEntity.status(401).build();
        notificationRepository.markAllRead(me.getId());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private User currentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        Object p = auth.getPrincipal();
        if (!(p instanceof UserDetails ud)) return null;
        return userRepository.findByEmail(ud.getUsername()).orElse(null);
    }
}
