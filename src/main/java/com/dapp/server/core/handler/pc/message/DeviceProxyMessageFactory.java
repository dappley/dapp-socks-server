package com.dapp.server.core.handler.pc.message;

import com.dapp.server.core.handler.pc.message.common.ProxyDataMessage;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;

/**
 * @author Admin
 */
public class DeviceProxyMessageFactory {
    public static DeviceProxyMessage encodeProxyMessage(MessageCode messageCode, Object object) {
        Gson gson = new Gson();
        String text = gson.toJson(object);

        DeviceProxyMessage deviceProxyMessage = new DeviceProxyMessage();
        deviceProxyMessage.setData(text.getBytes(StandardCharsets.UTF_8));
        deviceProxyMessage.setTag(0);
        deviceProxyMessage.setCode(messageCode.value());

        return deviceProxyMessage;
    }

    public static DeviceProxyMessage encodeProxyDataMessage(ProxyDataMessage proxyDataMessage) {
        byte[] idBytes = proxyDataMessage.getId().getBytes(StandardCharsets.UTF_8);
        int newLength = 4 + idBytes.length +   proxyDataMessage.getData().length;
        byte[] data = new byte[newLength];

        data[0] = (byte)((idBytes.length >> 24) & 0xFF);
        data[1] = (byte)((idBytes.length >> 16) & 0xFF);
        data[2] = (byte)((idBytes.length >> 8) & 0xFF);
        data[3] = (byte)(idBytes.length & 0xFF);

        System.arraycopy(idBytes, 0, data, 4, idBytes.length);
        System.arraycopy(proxyDataMessage.getData(), 0, data, idBytes.length + 4, proxyDataMessage.getData().length);

        DeviceProxyMessage deviceProxyMessage = new DeviceProxyMessage();
        deviceProxyMessage.setCode(MessageCode.PROXY_DATA.value());
        deviceProxyMessage.setTag(0);
        deviceProxyMessage.setData(data);

        return deviceProxyMessage;
    }

    public static ProxyDataMessage decodeProxyDataMessage(DeviceProxyMessage message) {
        byte[] data = message.getData();
        int idLength = ((data[0] & 0xFF) << 24) +
                ((data[1] & 0xFF) << 16) +
                ((data[2] & 0xFF) << 8) + (data[3] & 0xFF);

        String msgId = new String(data, 4, idLength, StandardCharsets.UTF_8);
        byte[] rawData = new byte[data.length - 4 - idLength];
        System.arraycopy(data, 4 + idLength, rawData, 0, rawData.length);

        return ProxyDataMessage.builder().id(msgId).data(rawData).build();
    }
}
