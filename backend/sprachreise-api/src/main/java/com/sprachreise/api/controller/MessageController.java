package com.sprachreise.api.controller;

import com.sprachreise.api.dto.ConversationDto;
import com.sprachreise.api.dto.MessageDto;
import com.sprachreise.api.entity.Message;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.MessageRepository;
import com.sprachreise.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Value("${storage.upload-dir}")
    private String storageDir;

    private final MessageRepository    messageRepository;
    private final UserRepository       userRepository;
    private final SimpMessagingTemplate broker;

    public MessageController(MessageRepository messageRepository,
                             UserRepository userRepository,
                             SimpMessagingTemplate broker) {
        this.messageRepository = messageRepository;
        this.userRepository    = userRepository;
        this.broker            = broker;
    }

    // ── Envoyer un message texte ─────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> send(@RequestBody Map<String, Object> body) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Object recipientObj = body.get("recipientId");
        if (recipientObj == null) return ResponseEntity.badRequest().body(Map.of("error","recipientId requis"));
        Long recipientId = Long.valueOf(recipientObj.toString());

        String content = body.containsKey("content") ? body.get("content").toString().trim() : "";
        String attachmentUrl  = body.containsKey("attachmentUrl")  ? (String) body.get("attachmentUrl")  : null;
        String attachmentType = body.containsKey("attachmentType") ? (String) body.get("attachmentType") : null;
        String attachmentName = body.containsKey("attachmentName") ? (String) body.get("attachmentName") : null;
        String messageType    = body.containsKey("messageType")    ? (String) body.get("messageType")    : "TEXT";
        Long replyToId = body.containsKey("replyToId") && body.get("replyToId") != null
            ? Long.valueOf(body.get("replyToId").toString()) : null;

        if (content.isBlank() && attachmentUrl == null)
            return ResponseEntity.badRequest().body(Map.of("error","Message vide"));

        User recipient = userRepository.findById(recipientId).orElse(null);
        if (recipient == null) return ResponseEntity.notFound().build();

        Message msg = new Message();
        msg.setSenderId(me.getId());
        msg.setRecipientId(recipientId);
        msg.setContent(content.isBlank() ? null : content);
        msg.setMessageType(messageType);
        msg.setReplyToId(replyToId);
        msg.setAttachmentUrl(attachmentUrl);
        msg.setAttachmentType(attachmentType);
        msg.setAttachmentName(attachmentName);
        messageRepository.save(msg);

        MessageDto dto = MessageDto.from(msg);
        // Résoudre le contenu de la réponse
        if (replyToId != null) {
            messageRepository.findById(replyToId).ifPresent(replied ->
                dto.replyToContent = replied.getContent() != null ? replied.getContent() : "[média]"
            );
        }

        // Broadcast temps réel → destinataire + expéditeur (pour multi-device)
        broker.convertAndSend("/topic/messages/" + recipientId, dto);
        broker.convertAndSend("/topic/messages/" + me.getId(), dto);
        // Mise à jour inbox (badge)
        broker.convertAndSend("/topic/inbox/" + recipientId, dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    // ── Upload fichier message (image, video, audio, document) ───────────────
    @PostMapping("/upload")
    public ResponseEntity<?> uploadAttachment(@RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "type", defaultValue = "FILE") String type) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
            String subDir = switch (type.toUpperCase()) {
                case "IMAGE" -> "messages/images";
                case "VIDEO" -> "messages/videos";
                case "AUDIO" -> "messages/audio";
                default      -> "messages/files";
            };
            String storedName = UUID.randomUUID() + ext;
            Path dest = Paths.get(storageDir).resolve(subDir);
            Files.createDirectories(dest);
            file.transferTo(dest.resolve(storedName).toFile());

            return ResponseEntity.ok(Map.of(
                "url",  subDir + "/" + storedName,
                "name", original,
                "type", type.toUpperCase()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Modifier le texte d'un message ───────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> edit(@PathVariable Long id, @RequestBody Map<String,Object> body) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Message msg = messageRepository.findById(id).orElse(null);
        if (msg == null) return ResponseEntity.notFound().build();
        if (!msg.getSenderId().equals(me.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","Non autorisé"));
        String newContent = body.containsKey("content") ? body.get("content").toString().trim() : null;
        if (newContent == null || newContent.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error","Contenu vide"));
        msg.setContent(newContent);
        msg.setEdited(true);
        messageRepository.save(msg);
        return ResponseEntity.ok(MessageDto.from(msg));
    }

    // ── Épingler / désépingler ────────────────────────────────────────────────
    @PostMapping("/{id}/pin")
    public ResponseEntity<?> pin(@PathVariable Long id) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Message msg = messageRepository.findById(id).orElse(null);
        if (msg == null) return ResponseEntity.notFound().build();
        // Les deux parties de la conversation peuvent épingler
        if (!msg.getSenderId().equals(me.getId()) && !msg.getRecipientId().equals(me.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","Non autorisé"));
        msg.setPinned(!Boolean.TRUE.equals(msg.getPinned()));
        messageRepository.save(msg);
        return ResponseEntity.ok(Map.of("id", id, "pinned", msg.getPinned()));
    }

    // ── Info (envoyé + lu) ────────────────────────────────────────────────────
    @GetMapping("/{id}/info")
    public ResponseEntity<?> info(@PathVariable Long id) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Message msg = messageRepository.findById(id).orElse(null);
        if (msg == null) return ResponseEntity.notFound().build();
        if (!msg.getSenderId().equals(me.getId()) && !msg.getRecipientId().equals(me.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","Non autorisé"));
        return ResponseEntity.ok(Map.of(
            "sentAt",  msg.getSentAt()  != null ? msg.getSentAt().toString()  : null,
            "readAt",  msg.getReadAt()  != null ? msg.getReadAt().toString()  : null,
            "edited",  Boolean.TRUE.equals(msg.getEdited()),
            "deleted", Boolean.TRUE.equals(msg.getDeleted())
        ));
    }

    // ── Supprimer son propre message ─────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Message msg = messageRepository.findById(id).orElse(null);
        if (msg == null) return ResponseEntity.notFound().build();
        if (!msg.getSenderId().equals(me.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","Non autorisé"));
        msg.setDeleted(true);
        msg.setContent(null);
        msg.setAttachmentUrl(null);
        messageRepository.save(msg);
        return ResponseEntity.ok(Map.of("id", id, "deleted", true));
    }

    // ── Conversation avec un utilisateur ────────────────────────────────────
    @GetMapping("/conversation/{otherUserId}")
    public ResponseEntity<?> conversation(@PathVariable Long otherUserId) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        messageRepository.markAsRead(me.getId(), otherUserId, LocalDateTime.now());

        List<Message> messages = messageRepository.findByPair(me.getId(), otherUserId);

        // Résoudre les contenus des messages cités
        Map<Long, String> replyContents = new HashMap<>();
        messages.stream()
            .filter(m -> m.getReplyToId() != null)
            .map(Message::getReplyToId)
            .distinct()
            .forEach(rid -> messageRepository.findById(rid).ifPresent(r ->
                replyContents.put(rid, r.getContent() != null ? r.getContent() : "[média]")
            ));

        List<MessageDto> dtos = messages.stream().map(m -> {
            MessageDto dto = MessageDto.from(m);
            if (m.getReplyToId() != null) dto.replyToContent = replyContents.get(m.getReplyToId());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ── Boîte de réception ────────────────────────────────────────────────────
    @GetMapping("/inbox")
    public ResponseEntity<?> inbox() {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Message> all = messageRepository.findAllForUser(me.getId());

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
            dto.lastMessage    = Boolean.TRUE.equals(last.getDeleted())
                ? "Message supprimé"
                : (last.getContent() != null ? last.getContent() : "[" + (last.getAttachmentType() != null ? last.getAttachmentType().toLowerCase() : "fichier") + "]");
            dto.lastSentAt     = last.getSentAt() != null ? last.getSentAt().toString() : null;
            dto.lastSenderId   = last.getSenderId();
            dto.unreadCount    = (int) messageRepository.countUnread(me.getId(), otherId);
            result.add(dto);
        }
        return ResponseEntity.ok(result);
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object p = auth.getPrincipal();
        String email = (p instanceof UserDetails ud) ? ud.getUsername() : p.toString();
        return userRepository.findByEmail(email).orElse(null);
    }
}
