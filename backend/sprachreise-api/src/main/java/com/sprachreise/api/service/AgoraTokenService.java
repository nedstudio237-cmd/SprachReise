package com.sprachreise.api.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Stub Agora token service for the MVP. Generates a fake token string
 * instead of calling the real Agora SDK. Replace with a real token
 * builder (agora-token-builder) when wiring the real-time SDK on mobile.
 */
@Service
public class AgoraTokenService {

    /**
     * Generates a stub token for the given channel.
     *
     * @param channelName the Agora channel name
     * @param uid         the user id joining (trainer or learner)
     * @param role        "HOST" or "AUDIENCE"
     * @return a placeholder token string
     */
    public String generateToken(String channelName, Long uid, String role) {
        return "agora_token_" + UUID.randomUUID();
    }
}
