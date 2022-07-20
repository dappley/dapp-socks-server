package com.dapp.server.core.handler.pc;

import com.dapp.server.protobuf.DeviceProxyMessageProto;
import com.dapp.server.protobuf.ProtobufTestProto;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Sun
 */

@Slf4j
public class ProtobufTestReqHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ProtobufTestProto.ProtobufTest ms = (ProtobufTestProto.ProtobufTest)msg;
        System.out.println(ms.getCode()+"======="+ms.getData()+"========="+ms.getMESSAGEHEADERLENGTH()+"========"+ms.getTag());
    }
    private DeviceProxyMessageProto.DeviceProxyMessage resp(int id){
        DeviceProxyMessageProto.DeviceProxyMessage.Builder builder = DeviceProxyMessageProto.DeviceProxyMessage.newBuilder();
        builder.setCode(id);
        return  builder.build();
    }

}
