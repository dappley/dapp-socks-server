package com.dapp.server.core.handler.socks;

import com.dapp.server.configuration.ProxyContextConfiguration;
import com.dapp.server.core.constants.SysConstants;
import com.dapp.server.core.handler.pc.message.DeviceProxyMessageFactory;
import com.dapp.server.core.handler.pc.message.MessageCode;
import com.dapp.server.core.handler.pc.message.common.ProxyDataMessage;
import com.dapp.server.core.handler.pc.message.server.ConnectRequest;
import com.dapp.server.core.handler.pc.message.server.DisconnectRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * @author Admin
 */
@Slf4j
public class Socks5CommandRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest>{
	private final static int MAX_DATA_LEN = 1500;

	private ProxyContextConfiguration proxyContextConfig;
	public Socks5CommandRequestHandler(ProxyContextConfiguration proxyContextConfig) {
	    this.proxyContextConfig = proxyContextConfig;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5CommandRequest msg) throws Exception {
		log.info("[Socks5CommandRequestHandler-channelRead0], command request dest server info : " + msg.type() + "," + msg.dstAddr() + "," + msg.dstPort());
		if(msg.type().equals(Socks5CommandType.CONNECT)) {
			if (!ctx.channel().hasAttr(SysConstants.USER_NAME_KEY)) {
				log.info("[[Socks5CommandRequestHandler-channelRead0]] fail, USER_NAME_KEY not found......");
				Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4);
				ctx.writeAndFlush(commandResponse).addListener(ChannelFutureListener.CLOSE);;
				return;
			}
			// UserName bound with client
			String userName = ctx.channel().attr(SysConstants.USER_NAME_KEY).get();
            ChannelHandlerContext clientContext = proxyContextConfig.getClientContext(userName);
            if (clientContext == null) {
				log.info("[[Socks5CommandRequestHandler-channelRead0]] fail, client <{}> proxy content is null......, please check connection is successful?", userName);
                Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4);
                ctx.writeAndFlush(commandResponse).addListener(ChannelFutureListener.CLOSE);
                return;
            }
            String connectionId = UUID.randomUUID().toString();
            ctx.channel().attr(SysConstants.CLIENT_ID_KEY).set(connectionId);
			proxyContextConfig.addSocksContext(connectionId, ctx);

			ConnectRequest connectRequest = ConnectRequest.builder()
					.id(connectionId)
					.dst(msg.dstAddr())
					.port(msg.dstPort())
					.build();
			ctx.channel().pipeline().addLast(new ProxyHandler(proxyContextConfig, connectionId, clientContext));
			clientContext.writeAndFlush(DeviceProxyMessageFactory.encodeProxyMessage(MessageCode.CONNECT, connectRequest));
		} else if (msg.type().equals(Socks5CommandType.BIND)) {
		    log.error("Not implementation");
            Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4);
            ctx.writeAndFlush(commandResponse).addListener(ChannelFutureListener.CLOSE);;
        } else if(msg.type().equals(Socks5CommandType.UDP_ASSOCIATE)) {
            log.error("Not implementation");
            Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4);
            ctx.writeAndFlush(commandResponse).addListener(ChannelFutureListener.CLOSE);;
        }
	}

	static class ProxyHandler extends SimpleChannelInboundHandler<ByteBuf> {
		private String connectionId;
		private ChannelHandlerContext clientContext;
		private ProxyContextConfiguration proxyContextConfig;

	    public ProxyHandler(ProxyContextConfiguration proxyContextConfig, String connectionId, ChannelHandlerContext clientContext) {
			this.connectionId = connectionId;
			this.clientContext = clientContext;
			this.proxyContextConfig = proxyContextConfig;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
	        int readLength = Math.min(msg.readableBytes(), MAX_DATA_LEN);
	        while (readLength > 0) {
                byte[] data = new byte[readLength];
                msg.readBytes(data);
                ProxyDataMessage proxyDataMessage = ProxyDataMessage.builder().id(connectionId).data(data).build();
                clientContext.writeAndFlush(DeviceProxyMessageFactory.encodeProxyDataMessage(proxyDataMessage));
                readLength = Math.min(msg.readableBytes(), MAX_DATA_LEN);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			super.channelInactive(ctx);
            DisconnectRequest disconnectRequest = new DisconnectRequest();
            disconnectRequest.setId(connectionId);
            clientContext.writeAndFlush(DeviceProxyMessageFactory.encodeProxyMessage(MessageCode.DISCONNECT, disconnectRequest));
			this.proxyContextConfig.removeSocksContext(connectionId);
			log.info("close a connection id {}", connectionId);
        }
    }
}
