package com.sprachreise.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serveur de signalisation WebRTC pour le live local.
 * Chaque session live a son propre "room" identifié par sessionId.
 * Échange: join → room-state → offer/answer/ice → peer-left
 * Tout le trafic vidéo/audio passe en P2P sur le réseau local via WebRTC.
 */
public class SignalingWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();

    // roomId → { peerId → WebSocketSession }
    private final Map<String, Map<String, WebSocketSession>> rooms = new ConcurrentHashMap<>();

    // wsSessionId → [peerId, roomId]
    private final Map<String, String[]> sessionMeta = new ConcurrentHashMap<>();

    // peerId → { name, isTrainer }
    private final Map<String, Map<String, Object>> peerInfo = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // attendre le message "join"
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> msg = mapper.readValue(message.getPayload(), Map.class);
        String type = (String) msg.get("type");
        if (type == null) return;

        switch (type) {
            case "join"        -> handleJoin(session, msg);
            case "offer"       -> handleRoute(session, msg);
            case "answer"      -> handleRoute(session, msg);
            case "ice"         -> handleRoute(session, msg);
            case "mute-state"  -> handleMuteState(session, msg);
            case "audio-relay" -> handleAudioRelay(session, msg);
            case "leave"       -> doLeave(session);
        }
    }

    // ── Join ─────────────────────────────────────────────────────────────────
    private void handleJoin(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String roomId    = str(msg, "roomId");
        String peerId    = str(msg, "peerId");
        String name      = str(msg, "name");
        boolean trainer  = Boolean.TRUE.equals(msg.get("isTrainer"));

        if (roomId == null || peerId == null) return;

        // Enregistrer
        Map<String, WebSocketSession> room = rooms.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());
        room.put(peerId, session);
        sessionMeta.put(session.getId(), new String[]{peerId, roomId});
        peerInfo.put(peerId, Map.of("name", name != null ? name : "?", "isTrainer", trainer));

        // Envoyer la liste des pairs existants au nouveau venu
        List<Map<String, Object>> existing = new ArrayList<>();
        for (String pid : room.keySet()) {
            if (!pid.equals(peerId)) {
                Map<String, Object> info = peerInfo.getOrDefault(pid, Map.of());
                existing.add(Map.of(
                    "peerId",    pid,
                    "name",      info.getOrDefault("name", "?"),
                    "isTrainer", info.getOrDefault("isTrainer", false)
                ));
            }
        }
        send(session, Map.of("type", "room-state", "peers", existing, "yourPeerId", peerId));

        // Notifier les autres de l'arrivée
        broadcast(roomId, peerId, Map.of(
            "type",      "peer-joined",
            "peerId",    peerId,
            "name",      name != null ? name : "?",
            "isTrainer", trainer
        ));
    }

    // ── Routage offer / answer / ice ─────────────────────────────────────────
    private void handleRoute(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String to = str(msg, "to");
        if (to == null) return;
        String[] meta = sessionMeta.get(session.getId());
        if (meta == null) return;
        Map<String, WebSocketSession> room = rooms.get(meta[1]);
        if (room == null) return;
        WebSocketSession target = room.get(to);
        if (target != null && target.isOpen()) {
            target.sendMessage(new TextMessage(mapper.writeValueAsString(msg)));
        }
    }

    // ── Relay audio (chunks base64) ───────────────────────────────────────────
    private void handleAudioRelay(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String[] meta = sessionMeta.get(session.getId());
        if (meta == null) return;
        Map<String, Object> enriched = new HashMap<>(msg);
        enriched.put("peerId", meta[0]);
        broadcast(meta[1], meta[0], enriched);
    }

    // ── Diffuser l'état mute/caméra ──────────────────────────────────────────
    private void handleMuteState(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String[] meta = sessionMeta.get(session.getId());
        if (meta == null) return;
        Map<String, Object> enriched = new HashMap<>(msg);
        enriched.put("peerId", meta[0]);
        broadcast(meta[1], meta[0], enriched);
    }

    // ── Quitter ──────────────────────────────────────────────────────────────
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        doLeave(session);
    }

    private void doLeave(WebSocketSession session) throws IOException {
        String[] meta = sessionMeta.remove(session.getId());
        if (meta == null) return;
        String peerId = meta[0];
        String roomId = meta[1];
        peerInfo.remove(peerId);
        Map<String, WebSocketSession> room = rooms.get(roomId);
        if (room != null) {
            room.remove(peerId);
            if (room.isEmpty()) rooms.remove(roomId);
        }
        broadcast(roomId, peerId, Map.of("type", "peer-left", "peerId", peerId));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void send(WebSocketSession session, Object data) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(mapper.writeValueAsString(data)));
        }
    }

    private void broadcast(String roomId, String excludePeerId, Map<String, Object> data) throws IOException {
        Map<String, WebSocketSession> room = rooms.get(roomId);
        if (room == null) return;
        String json = mapper.writeValueAsString(data);
        for (Map.Entry<String, WebSocketSession> e : room.entrySet()) {
            if (!e.getKey().equals(excludePeerId) && e.getValue().isOpen()) {
                e.getValue().sendMessage(new TextMessage(json));
            }
        }
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof String s ? s : null;
    }
}
