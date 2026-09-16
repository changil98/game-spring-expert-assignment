package com.gameexpert.player.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class CreatePlayerRequest {

    @NotBlank
    @Size(min = 2, max = 12)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    private final String nickname;

    public CreatePlayerRequest(String nickname) {
        this.nickname = nickname;
    }
}
