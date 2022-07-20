package com.dapp.server.core.handler.pc.message;

import lombok.Data;

@Data
public class ProtobufTest {

    public static int MESSAGE_HEADER_LENGTH=12;
    /**
     * Message
     */
    private int code;

    private int tag;

    private byte[] data;
}
