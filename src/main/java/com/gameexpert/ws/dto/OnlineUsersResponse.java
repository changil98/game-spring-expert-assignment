package com.gameexpert.ws.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class OnlineUsersResponse {
    private final List<String> users;
    private final int count;

    private final String type = "onlineUsers";

    public OnlineUsersResponse(List<String> users, int count) {
        this.users = users;
        this.count = count;
    }
}
