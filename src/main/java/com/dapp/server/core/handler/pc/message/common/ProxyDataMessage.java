package com.dapp.server.core.handler.pc.message.common;

import lombok.Builder;
import lombok.Data;

/**
 * @author Admin
 */
@Data
@Builder
public class ProxyDataMessage {
    private String id;
    private byte[] data;
}
