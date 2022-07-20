package com.dapp.server.core.handler.pc.message.response;

public class PasswordAuthStatus implements Comparable<PasswordAuthStatus> {

    public static final PasswordAuthStatus SUCCESS = new PasswordAuthStatus(0x00, "SUCCESS");
    public static final PasswordAuthStatus FAILURE = new PasswordAuthStatus(0xFF, "FAILURE");

    public static PasswordAuthStatus valueOf(byte b) {
        switch (b) {
            case 0x00:
                return SUCCESS;
            case (byte) 0xFF:
                return FAILURE;
        }

        return new PasswordAuthStatus(b);
    }

    private final byte byteValue;
    private final String name;
    private String text;

    public PasswordAuthStatus(int byteValue) {
        this(byteValue, "UNKNOWN");
    }

    public PasswordAuthStatus(int byteValue, String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }

        this.byteValue = (byte) byteValue;
        this.name = name;
    }

    public byte byteValue() {
        return byteValue;
    }

    public boolean isSuccess() {
        return byteValue == 0;
    }

    @Override
    public int hashCode() {
        return byteValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PasswordAuthStatus)) {
            return false;
        }

        return byteValue == ((PasswordAuthStatus) obj).byteValue;
    }

    @Override
    public int compareTo(PasswordAuthStatus o) {
        return byteValue - o.byteValue;
    }

    @Override
    public String toString() {
        String text = this.text;
        if (text == null) {
            this.text = text = name + '(' + (byteValue & 0xFF) + ')';
        }
        return text;
    }

}
