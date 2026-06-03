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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
public class MessagesController {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessagesController(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> send(@RequestBody Map<String, Object> body) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Object recipientObj = body.get("recipientId");
        Object contentObj = body.get("content");
        if (recipientObj == null || contentObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "recipientId et content requis"));
        }
        Long recipientId;
        try {
            recipientId = Long.parseLong(String.valueOf(recipientObj));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "recipientId invalide"));
        }
        String content = String.valueOf(contentObj).trim();
        if (content.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message vide"));
        }
        if (Objects.equals(recipientId, me.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Impossible de s'envoyer un message"));
        }
        if (userRepository.findById(recipientId).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Destinataire introuvable"));
        }

        Message m = new Message();
        m.setSenderId(me.getId());
        m.setRecipientId(recipientId);
        m.setContent(content);
        messageRepository.save(m);
        return ResponseEntity.ok(MessageDto.from(m));
    }

    @GetMapping("/conversation/{otherUserId}")
    @Transactional
    public ResponseEntity<?> conversation(@PathVariable Long otherUserId) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // Mark inbound messages as read
        messageRepository.markAsRead(me.getId(), otherUserId, LocalDateTime.now());

        List<Message> msgs = messageRepository.findByPair(me.getId(), otherUserId);
        List<MessageDto> dtos = msgs.stream().map(MessageDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/inbox")
    public ResponseEntity<?> inbox() {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Message> all = messageRepository.findAllForUser(me.getId());
        // Group by other-user id, keep newest first per partner
        Map<Long, Message> latestByPartner = new LinkedHashMap<>();
        for (Message m : all) {
            Long other = m.getSenderId().equals(me.getId()) ? m.getRecipientId() : m.getSenderId();
            if (!latestByPartner.containsKey(other)) {
                latestByPartner.put(other, m);
            }
        }

        List<ConversationDto> result = new ArrayList<>();
        for (Map.Entry<Long, Message> entry : latestByPartner.entrySet()) {
            Long otherId = entry.getKey();
            Message last = entry.getValue();
            User other = userRepository.findById(otherId).orElse(null);
            if (other == null) continue;

            ConversationDto dto = new ConversationDto();
            dto.otherUserId = other.getId();
            dto.otherFirstName = other.getFirstName();
            dto.otherLastName = other.getLastName();
            dto.otherPhotoUrl = other.getPhotoUrl();
            dto.otherRole = other.getRole() != null ? other.getRole().name() : null;
            dto.lastMessage = last.getContent();
            dto.lastSentAt = last.getSentAt() != null ? last.getSentAt().toString() : null;
            dto.lastSenderId = last.getSenderId();
            dto.unreadCount = (int) messageRepository.countUnread(me.getId(), otherId);
            result.add(dto);
        }
        return ResponseEntity.ok(result);
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) return null;
        Object principal = auth.getPrincipal();
        if (!(principal instanceof UserDetails)) return null;
        String email = ((UserDetails) principal).getUsername();
        return userRepository.findByEmail(email).orElse(null);
    }
}
