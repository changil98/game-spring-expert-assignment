package com.gameexpert.ws.handler;

import com.gameexpert.api.SessionRegistry;
import com.gameexpert.ws.NicknameHandshakeInterceptor;
import com.gameexpert.ws.WorldBroadcaster;
import com.gameexpert.ws.WorldSessionRegistry;
import com.gameexpert.ws.WsMessageContext;
import com.gameexpert.ws.dto.OnlineUsersResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OnlineUsersWsHandler implements WsMessageHandler {
    private final WorldSessionRegistry registry;
    private final WorldBroadcaster broadcaster;

    @Override
    public String type() {
        return "onlineUsers";
    }

    @Override
    public void handle(WsMessageContext context, JsonNode message) {
        Collection<SessionRegistry.Entry> entries = registry.entries(context.worldId());
        List<String> users = new ArrayList<>();
        for (SessionRegistry.Entry entry : entries) {
            WebSocketSession session = entry.session();
            if (session.isOpen()) {
                Map<String, Object> attributes = session.getAttributes();
                users.add(session.getAttributes().get(NicknameHandshakeInterceptor.ATTR_NICKNAME).toString());
            }
        }
        users.sort(String::compareTo);
        OnlineUsersResponse onlineUsers = new OnlineUsersResponse(users, users.size());
        broadcaster.sendTo(context.session(), onlineUsers);
    }
}
