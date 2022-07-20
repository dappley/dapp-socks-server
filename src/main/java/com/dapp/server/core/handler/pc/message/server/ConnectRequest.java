package com.dapp.server.core.handler.pc.message.server;

import lombok.Builder;
import lombok.Data;

/**
 * @author Admin
 */
@Data
@Builder
public class ConnectRequest {
    private String id;
    private String dst;
    private int    port;
}
