package com.dapp.server.core.handler.pc.encoder;

import com.dapp.server.core.handler.pc.message.DeviceProxyMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Admin
 */
@Slf4j
public class ClientMessageEncoder extends MessageToByteEncoder<DeviceProxyMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, DeviceProxyMessage msg, ByteBuf out) throws Exception {
        if (msg.getData() != null) {
            out.writeInt(msg.getData().length);
        } else {
            out.writeInt(0);
        }

        out.writeInt(msg.getCode());
        out.writeInt(msg.getTag());
        if (msg.getData() != null) {
            out.writeBytes(msg.getData());
        }
    }

}
