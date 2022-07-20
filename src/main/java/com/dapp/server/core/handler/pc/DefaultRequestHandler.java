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
import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
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
public class DefaultRequestHandler extends SimpleChannelInboundHandler<DeviceProxyMessage> {

    private ProxyContextConfiguration proxyContextConfig;

    public DefaultRequestHandler(ProxyContextConfiguration proxyContextConfig) {
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
    protected void channelRead0(ChannelHandlerContext ctx, DeviceProxyMessage msg) throws Exception {
        MessageCode messageCode;
        try {
            messageCode = MessageCode.value2MessageCode(msg.getCode());
        } catch (MessageCodeException e) {
            return;
        }
        switch (messageCode)
        {
            case LOGIN:
                login(ctx, msg);
                break;
            case HEARTBEAT:
                heartbeat(ctx, msg);
                break;
            case CONNECT:
                connect(ctx, msg);
                break;
            case PROXY_DATA:
                passData(ctx, msg);
                break;
            case DISCONNECT:
                disconnect(ctx, msg);
                break;
            default:
                log.error("Impossible");
        }
    }

    private void login(ChannelHandlerContext ctx, DeviceProxyMessage msg) {
        Gson gson = new Gson();
        LoginRequest request = gson.fromJson(new String(msg.getData(), StandardCharsets.UTF_8), LoginRequest.class);
        if (Objects.isNull(request.getUserName()) || request.getUserName().length() == 0) {
            request.setUserName(UUID.randomUUID().toString());
        }
        proxyContextConfig.addClientContext(request.getUserName(), ctx);
        ctx.channel().attr(SysConstants.USER_NAME_KEY).set(request.getUserName());
        LoginResponse response = new LoginResponse();
        response.setError(ErrorCode.SUCCESS);
        response.setUsername(request.getUserName());
        ctx.writeAndFlush(DeviceProxyMessageFactory.encodeProxyMessage(MessageCode.LOGIN, response));
        log.info("Start a persistent connection success, client id is {}", request.getUserName());
    }

    private void connect(ChannelHandlerContext ctx, DeviceProxyMessage msg) {
        Gson gson = new Gson();
        ConnectResponse response = gson.fromJson(new String(msg.getData(), StandardCharsets.UTF_8), ConnectResponse.class);
        ChannelHandlerContext proxyContext = proxyContextConfig.getSocksContext(response.getId());
        if (proxyContext == null) {
            log.error("Connection id {} not found", response.getId());
            DisconnectRequest disconnectRequest = new DisconnectRequest();
            disconnectRequest.setId(response.getId());
            ctx.writeAndFlush(DeviceProxyMessageFactory.encodeProxyMessage(MessageCode.DISCONNECT, disconnectRequest));
            return;
        }
        if (response.getResult() == ErrorCode.FAIL) {
            Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4);
            proxyContext.writeAndFlush(commandResponse).addListener(ChannelFutureListener.CLOSE);
        } else {
            log.info("Connection id {} is active, wait to send ......", response.getId());
            InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
            Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(
                    Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4, address.getHostString(), address.getPort());
            proxyContext.writeAndFlush(commandResponse);
        }
    }

    private void passData(ChannelHandlerContext ctx, DeviceProxyMessage msg) {
        ProxyDataMessage dataMessage = DeviceProxyMessageFactory.decodeProxyDataMessage(msg);
        ChannelHandlerContext proxyContext = proxyContextConfig.getSocksContext(dataMessage.getId());
        if (proxyContext == null) {
            log.error("DefaultRequestHandler-passData, connection id {} not found", dataMessage.getId());
            return;
        }
        ByteBuf byteBuf = proxyContext.alloc().buffer();
        byteBuf.writeBytes(dataMessage.getData());
        proxyContext.writeAndFlush(byteBuf);
    }

    private void disconnect(ChannelHandlerContext ctx, DeviceProxyMessage msg) {
        Gson gson = new Gson();
        DisconnectRequest request = gson.fromJson(new String(msg.getData(), StandardCharsets.UTF_8), DisconnectRequest.class);
        ChannelHandlerContext clientContext = proxyContextConfig.getSocksContext(request.getId());
        if (clientContext != null) {
            log.error("[DefaultRequestHandler-disconnect], close a connection is：{}", request.getId());
            clientContext.fireChannelInactive();
            proxyContextConfig.removeSocksContext(request.getId());
        }
    }

    private void heartbeat(ChannelHandlerContext ctx, DeviceProxyMessage msg) {
        Gson gson = new Gson();
        HeartbeatRequest request = gson.fromJson(new String(msg.getData(), StandardCharsets.UTF_8), HeartbeatRequest.class);
        ChannelHandlerContext clientContext = proxyContextConfig.getClientContext(request.getUserName());
        HeartbeatResponse response = new HeartbeatResponse();
        if (clientContext == null) {
            response.setError(ErrorCode.FAIL);
            ctx.writeAndFlush(DeviceProxyMessageFactory.encodeProxyMessage(MessageCode.HEARTBEAT, response))
                    .addListener(ChannelFutureListener.CLOSE);
        } else {
            response.setError(ErrorCode.SUCCESS);
            ctx.writeAndFlush(DeviceProxyMessageFactory.encodeProxyMessage(MessageCode.HEARTBEAT, response));
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
