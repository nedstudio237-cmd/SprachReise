package com.sprachreise.api.controller;

import com.sprachreise.api.dto.ConversationDto;
import com.sprachreise.api.dto.MessageDto;
import com.sprachreise.api.entity.Message;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.MessageRepository;
import com.sprachreise.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageController(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    // ── Envoyer un message ───────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> send(@RequestBody Map<String, Object> body) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Long recipientId = Long.valueOf(body.get("recipientId").toString());
        String content   = body.get("content").toString().trim();
        if (content.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Message vide"));

        User recipient = userRepository.findById(recipientId).orElse(null);
        if (recipient == null) return ResponseEntity.notFound().build();

        Message msg = new Message();
        msg.setSenderId(me.getId());
        msg.setRecipientId(recipientId);
        msg.setContent(content);
        messageRepository.save(msg);
        return ResponseEntity.status(HttpStatus.CREATED).body(MessageDto.from(msg));
    }

    // ── Conversation avec un utilisateur ────────────────────────────────────
    @GetMapping("/conversation/{otherUserId}")
    public ResponseEntity<?> conversation(@PathVariable Long otherUserId) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // Marquer les messages reçus comme lus
        messageRepository.markAsRead(me.getId(), otherUserId, LocalDateTime.now());

        List<MessageDto> msgs = messageRepository.findByPair(me.getId(), otherUserId)
                .stream().map(MessageDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(msgs);
    }

    // ── Boîte de réception (liste des conversations) ─────────────────────────
    @GetMapping("/inbox")
    public ResponseEntity<?> inbox() {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Message> all = messageRepository.findAllForUser(me.getId());

        // Regrouper par interlocuteur (otherUserId)
        Map<Long, Message> lastByOther = new LinkedHashMap<>();
        for (Message m : all) {
            Long otherId = m.getSenderId().equals(me.getId()) ? m.getRecipientId() : m.getSenderId();
            lastByOther.putIfAbsent(otherId, m);
        }

        List<ConversationDto> result = new ArrayList<>();
        for (Map.Entry<Long, Message> entry : lastByOther.entrySet()) {
            Long otherId = entry.getKey();
            Message last = entry.getValue();
            User other = userRepository.findById(otherId).orElse(null);
            if (other == null) continue;

            ConversationDto dto = new ConversationDto();
            dto.otherUserId    = otherId;
            dto.otherFirstName = other.getFirstName();
            dto.otherLastName  = other.getLastName();
            dto.otherPhotoUrl  = other.getPhotoUrl();
            dto.otherRole      = other.getRole().name();
            dto.lastMessage    = last.getContent();
            dto.lastSentAt     = last.getSentAt() != null ? last.getSentAt().toString() : null;
            dto.lastSenderId   = last.getSenderId();
            dto.unreadCount    = (int) messageRepository.countUnread(me.getId(), otherId);
            result.add(dto);
        }
        return ResponseEntity.ok(result);
    }

    // ── Helper ───────────────────────────────────────────────────────────────
    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        String email = (principal instanceof UserDetails ud) ? ud.getUsername() : principal.toString();
        return userRepository.findByEmail(email).orElse(null);
    }
}
