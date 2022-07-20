package com.dapp.server.core.handler.pc.message.client;

import lombok.Data;

@Data
public class LoginResponse {
    private int error;
    private String username;
}
