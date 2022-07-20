package com.dapp.server.core.handler.pc.message;


import com.dapp.server.core.exceptions.MessageCodeException;

/**
 * @author Admin
 */

public enum MessageCode {
    /**
     * 消息枚举类型
     */
    LOGIN(0),
    HEARTBEAT(1),
    CONNECT(2),
    PROXY_DATA(3),
    DISCONNECT(4);

    private int value;

    MessageCode(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static MessageCode value2MessageCode(int value) {
        if (value >= MessageCode.values().length) {
            throw new MessageCodeException("Invalid message code " + value);
        }
        return MessageCode.values()[value];
    }

}
