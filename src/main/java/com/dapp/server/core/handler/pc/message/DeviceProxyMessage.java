package com.dapp.server.core.handler.pc.message;

import lombok.Data;

/**
 * @author Admin
 */
@Data
public class DeviceProxyMessage {
    /**
     * Message Header Length 12 Byte
     * length | code   |  tag
     * 4Byte  | 4Byte  | 4Byte
     * length, exclude the message header,
     */
    public static int MESSAGE_HEADER_LENGTH = 12;
    /**
     * Message
     */
    private int code;

    private int tag;

    private byte[] data;
}
