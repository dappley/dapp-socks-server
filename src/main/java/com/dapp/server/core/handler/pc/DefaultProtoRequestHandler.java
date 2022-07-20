package com.dapp.server.core.handler.pc;

import com.dapp.server.configuration.ProxyContextConfiguration;
import com.dapp.server.core.constants.ErrorCode;
import com.dapp.server.core.constants.SysConstants;
import com.dapp.server.core.exceptions.MessageCodeException;
import com.dapp.server.core.handler.pc.message.DeviceProxyMessage;
import com.dapp.server.core.handler.pc.message.DeviceProxyMessageFactory;
import com.dapp.server.core.handler.pc.message.MessageCode;
import com.dapp.server.core.handler.pc.message.client.HeartbeatRequest;
import com.dapp.server.core.handler.pc.message.client.HeartbeatResponse;
import com.dapp.server.core.handler.pc.message.client.LoginRequest;
import com.dapp.server.core.handler.pc.message.client.LoginResponse;
import com.dapp.server.core.handler.pc.message.common.ProxyDataMessage;
import com.dapp.server.core.handler.pc.message.server.ConnectResponse;
import com.dapp.server.core.handler.pc.message.server.DisconnectRequest;
import com.dapp.server.protobuf.DeviceProxyMessageProto;
import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Admin
 */
@Slf4j
public class DefaultProtoRequestHandler extends ChannelInboundHandlerAdapter {

    private ProxyContextConfiguration proxyContextConfig;

    DeviceProxyMessageProto.DeviceProxyMessage.Builder builder = DeviceProxyMessageProto.DeviceProxyMessage.newBuilder();

    public DefaultProtoRequestHandler(ProxyContextConfiguration proxyContextConfig) {
        this.proxyContextConfig = proxyContextConfig;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        closeClientConnection(ctx);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        DeviceProxyMessageProto.DeviceProxyMessage protoMsg = (DeviceProxyMessageProto.DeviceProxyMessage)msg;
        MessageCode messageCode;
        try {
            messageCode = MessageCode.value2MessageCode(protoMsg.getCode());
        } catch (MessageCodeException e) {
            return;
        }
        switch (messageCode)
        {
            case LOGIN:
                login(ctx, protoMsg);
                break;
            case HEARTBEAT:
                heartbeat(ctx, protoMsg);
                break;
            case CONNECT:
                connect(ctx, protoMsg);
                break;
            case PROXY_DATA:
                passData(ctx, protoMsg);
                break;
            case UDP_ASSOCIATE:
                udp(ctx, protoMsg);
                break;
            case DISCONNECT:
                disconnect(ctx, protoMsg);
                break;
            default:
                log.error("Impossible");
        }
    }



    private void udp(ChannelHandlerContext ctx, DeviceProxyMessageProto.DeviceProxyMessage msg) {
        ChannelHandlerContext proxyContext = proxyContextConfig.getSocksContext(msg.getId());
        if (proxyContext == null) {
            log.error("DefaultRequestHandler-udp, connection id {} not found", msg.getId());
            return;
        }
        ByteBuf byteBuf = proxyContext.alloc().buffer();
        byteBuf.writeBytes(msg.getData().toByteArray());
        proxyContext.writeAndFlush(byteBuf);
    }

    private void login(ChannelHandlerContext ctx, DeviceProxyMessageProto.DeviceProxyMessage msg) {
//        Gson gson = new Gson();
//        LoginRequest request = gson.fromJson(new String(msg.getData(), StandardCharsets.UTF_8), LoginRequest.class);
        if (Objects.isNull(msg.getUserName()) || msg.getUserName().length() == 0) {
//            request.setUserName(UUID.randomUUID().toString());
              msg = builder.setUserName(UUID.randomUUID().toString()).build();
              builder.clear();
        }
        System.out.println("dprhandlermsg.getUserName()"+msg.getUserName());
        proxyContextConfig.addClientContext(msg.getUserName(), ctx);
        ctx.channel().attr(SysConstants.USER_NAME_KEY).set(msg.getUserName());
//        LoginResponse response = new LoginResponse();
//        response.setError(ErrorCode.SUCCESS);
//        response.setUsername(msg.getUserName());
        //        ctx.writeAndFlush(DeviceProxyMessageFactory.encodeProxyMessage(MessageCode.LOGIN, response));

        builder.setCode(MessageCode.LOGIN.value());
        builder.setError(ErrorCode.SUCCESS);

        ctx.writeAndFlush(builder.build());
        builder.clear();
        log.info("Start a persistent connection success, client id is {}", msg.getUserName());
    }

    private void connect(ChannelHandlerContext ctx, DeviceProxyMessageProto.DeviceProxyMessage msg) {
//        Gson gson = new Gson();
//        ConnectResponse response = gson.fromJson(new String(msg.getData(), StandardCharsets.UTF_8), ConnectResponse.class);
        ChannelHandlerContext proxyContext = proxyContextConfig.getSocksContext(msg.getId());
        if (proxyContext == null) {
            log.error("Connection id {} not found", msg.getId());
//            DisconnectRequest disconnectRequest = new DisconnectRequest();
//            disconnectRequest.setId(msg.getId());
//            ctx.writeAndFlush(DeviceProxyMessageFactory.encodeProxyMessage(MessageCode.DISCONNECT, disconnectRequest));


            builder.setCode(MessageCode.DISCONNECT.value());
            ctx.writeAndFlush(builder.build());
            builder.clear();

            return;
        }
        if (msg.getError() == ErrorCode.FAIL) {
            Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4);
            proxyContext.writeAndFlush(commandResponse).addListener(ChannelFutureListener.CLOSE);
        } else {
            log.info("Connection id {} is active, wait to send ......", msg.getId());
            InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
            Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(
                    Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4, address.getHostString(), address.getPort());
            proxyContext.writeAndFlush(commandResponse);
        }
    }

    private void passData(ChannelHandlerContext ctx, DeviceProxyMessageProto.DeviceProxyMessage msg) {
//        byte[] byteArray = msg.getData().toByteArray();
//        ProxyDataMessage dataMessage = DeviceProxyMessageFactory.decodeProxyDataByteMessage(byteArray);
        ChannelHandlerContext proxyContext = proxyContextConfig.getSocksContext(msg.getId());
        if (proxyContext == null) {
            log.error("DefaultRequestHandler-passData, connection id {} not found", msg.getId());
            return;
        }
        ByteBuf byteBuf = proxyContext.alloc().buffer();
        byteBuf.writeBytes(msg.getData().toByteArray());
//        System.out.println("server.transferData--------------------------------"+msg.getData().toStringUtf8());
        proxyContext.writeAndFlush(byteBuf);
    }

    private void disconnect(ChannelHandlerContext ctx, DeviceProxyMessageProto.DeviceProxyMessage msg) {
//        Gson gson = new Gson();
//        DisconnectRequest request = gson.fromJson(new String(msg.getData(), StandardCharsets.UTF_8), DisconnectRequest.class);
        ChannelHandlerContext clientContext = proxyContextConfig.getSocksContext(msg.getId());
        if (clientContext != null) {
            log.error("[DefaultRequestHandler-disconnect], close a connection is：{}", msg.getId());
            clientContext.fireChannelInactive();
            proxyContextConfig.removeSocksContext(msg.getId());
        }
    }

    private void heartbeat(ChannelHandlerContext ctx, DeviceProxyMessageProto.DeviceProxyMessage msg) {
//        Gson gson = new Gson();
//        HeartbeatRequest request = gson.fromJson(new String(msg.getData(), StandardCharsets.UTF_8), HeartbeatRequest.class);
        ChannelHandlerContext clientContext = proxyContextConfig.getClientContext(msg.getUserName());
//        HeartbeatResponse response = new HeartbeatResponse();
        if (clientContext == null) {
//            response.setError(ErrorCode.FAIL);

            builder.setError(ErrorCode.FAIL);

//            ctx.writeAndFlush(DeviceProxyMessageFactory.encodeProxyMessage(MessageCode.HEARTBEAT, response))

            ctx.writeAndFlush(builder.build())
                    .addListener(ChannelFutureListener.CLOSE);
            builder.clear();
        } else {
//            response.setError(ErrorCode.SUCCESS);

            builder.setError(ErrorCode.SUCCESS);
            builder.setCode(MessageCode.HEARTBEAT.value());
            builder.setTag(0);
//            ctx.writeAndFlush(DeviceProxyMessageFactory.encodeProxyMessage(MessageCode.HEARTBEAT, response));
            ctx.writeAndFlush(builder.build());
            builder.clear();
        }
    }

    private void closeClientConnection(ChannelHandlerContext ctx) {
        AttributeKey<String> userNameAttr = SysConstants.USER_NAME_KEY;
        if (ctx.channel().hasAttr(userNameAttr)) {
            String username = ctx.channel().attr(userNameAttr).get();
            proxyContextConfig.removeClientContext(username);
            log.info("Close a persistent connection success, client id is {}", username);
        }
    }
}
