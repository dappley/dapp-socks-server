package com.dapp.server.core.handler.pc.message.request;

import lombok.*;

/**
 * @author Admin
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PasswordRequest {

    private String username;
    private String password;

}
