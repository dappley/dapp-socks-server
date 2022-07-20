package com.dapp.server.core.handler.pc.message.request;

import lombok.Data;

/**
 * @author Admin
 */
@Data
public class ProxyDataRequest {
    private String id;
    private byte[] data;
}
