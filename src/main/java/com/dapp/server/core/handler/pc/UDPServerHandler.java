package com.dapp.server.core.handler.pc;

import com.dapp.server.configuration.ProxyContextConfiguration;
import com.dapp.server.protobuf.DeviceProxyMessageProto;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

import java.util.concurrent.ThreadLocalRandom;

public class UDPServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {


    private String connectionId;
    private ChannelHandlerContext clientContext;
    private ProxyContextConfiguration proxyContextConfig;
    DeviceProxyMessageProto.DeviceProxyMessage.Builder builder = DeviceProxyMessageProto.DeviceProxyMessage.newBuilder();

    public static final String[] d={"白日依山尽，黄河入海流。","床前明月光，疑是地上霜。","莫学武陵人，暂游桃源里。",
            "遥知兄弟登高处，遍插茱萸少一人。","正是江南好风景，落花时节又逢君。"};


    private String nextQuote(){
        int quoteid = ThreadLocalRandom.current().nextInt(d.length);
        return  d[quoteid];
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        String req = msg.content().toString(CharsetUtil.UTF_8);
        System.out.println(req);
        if("谚语字典查询？".equals(req)){
            ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer("谚语查询结果："+nextQuote(),CharsetUtil.UTF_8),msg.sender()));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel();
        cause.printStackTrace();
    }
}
