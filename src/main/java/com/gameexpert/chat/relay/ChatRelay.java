package com.gameexpert.chat.relay;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.gameexpert.chat.service.LocalChatSender;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatRelay implements MessageListener {
    public static final String CHANNEL = "webcraft:chat";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final LocalChatSender localChatSender;

    public void publish(Long worldId, Object message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("worldId", worldId);
        map.put("message", message);
        String messageMapping = objectMapper.writeValueAsString(map);
        redisTemplate.convertAndSend(CHANNEL, messageMapping);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        JsonNode node = objectMapper.readTree(body);
        Long worldId = node.get("worldId").asLong();
        JsonNode messageNode = node.get("message");
        localChatSender.send(worldId, messageNode);
    }
}
