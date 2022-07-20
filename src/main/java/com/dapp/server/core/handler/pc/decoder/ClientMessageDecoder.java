package com.dapp.server.core.handler.pc.decoder;

import com.dapp.server.core.handler.pc.message.DeviceProxyMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author Admin
 */
@Slf4j
public class ClientMessageDecoder extends ByteToMessageDecoder  {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        in.markReaderIndex();

        //Waiting for rest bytes
        if (in.readableBytes() < DeviceProxyMessage.MESSAGE_HEADER_LENGTH) {
            in.resetReaderIndex();
            return;
        }

        int messageLength = in.readInt();
        //Waiting for rest bytes
        if (in.readableBytes() < messageLength + DeviceProxyMessage.MESSAGE_HEADER_LENGTH - 4) {
            in.resetReaderIndex();
            return;
        }

        DeviceProxyMessage deviceProxyMessage = new DeviceProxyMessage();
        deviceProxyMessage.setCode(in.readInt());
        deviceProxyMessage.setTag(in.readInt());
        byte[] data = new byte[messageLength];
        in.readBytes(data);
        deviceProxyMessage.setData(data);
        out.add(deviceProxyMessage);
    }
}
