package com.dapp.server.core.handler.socks;

import com.dapp.server.configuration.ProxyContextConfiguration;
import com.dapp.server.core.constants.SysConstants;
import com.dapp.server.core.handler.pc.UDPserver;
import com.dapp.server.core.handler.pc.message.DeviceProxyMessageFactory;
import com.dapp.server.core.handler.pc.message.MessageCode;
import com.dapp.server.core.handler.pc.message.common.ProxyDataMessage;
import com.dapp.server.core.handler.pc.message.server.ConnectRequest;
import com.dapp.server.core.handler.pc.message.server.DisconnectRequest;
import com.dapp.server.protobuf.DeviceProxyMessageProto;
import com.google.protobuf.ByteString;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Admin
 */
@Slf4j
public class Socks5CommandRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest>{
	private final static int MAX_DATA_LEN = 1500;

	private ProxyContextConfiguration proxyContextConfig;


	DeviceProxyMessageProto.DeviceProxyMessage.Builder builder = DeviceProxyMessageProto.DeviceProxyMessage.newBuilder();


	protected AtomicLong consumeMsgLength;
	protected Runnable counterTask;
	protected final int M = 1024 * 1024;
	protected final int KB = 1024;

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
			System.out.println("s5.attr()"+ctx.channel().attr(SysConstants.USER_NAME_KEY).get());
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

//			ConnectRequest connectRequest = ConnectRequest.builder()
//					.id(connectionId)
//					.dst(msg.dstAddr())
//					.port(msg.dstPort())
//					.build();
			ctx.channel().pipeline().addLast(new ProxyHandler(proxyContextConfig, connectionId, clientContext));
//			clientContext.writeAndFlush(DeviceProxyMessageFactory.encodeProxyMessage(MessageCode.CONNECT, connectRequest));


			builder.setCode(MessageCode.CONNECT.value());
			builder.setTag(0);
			builder.setId(connectionId);
			builder.setDst(msg.dstAddr());
			builder.setPort(msg.dstPort());
			clientContext.writeAndFlush(builder.build());
			builder.clear();

		} else if (msg.type().equals(Socks5CommandType.BIND)) {
			log.error("Not implementation");
			Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4);
			ctx.writeAndFlush(commandResponse).addListener(ChannelFutureListener.CLOSE);;
		} else if(msg.type().equals(Socks5CommandType.UDP_ASSOCIATE)) {

			String userName = ctx.channel().attr(SysConstants.USER_NAME_KEY).get();
			ChannelHandlerContext clientContext = proxyContextConfig.getClientContext(userName);

			String connectionId = UUID.randomUUID().toString();
			ctx.channel().attr(SysConstants.CLIENT_ID_KEY).set(connectionId);
			proxyContextConfig.addSocksContext(connectionId, ctx);

			/**TODO
			 * 根据生产情况更换handler
			 */
			ctx.channel().pipeline().addLast(new ProxyHandler(proxyContextConfig, connectionId, clientContext));
			builder.setCode(MessageCode.UDP_ASSOCIATE.value());
			builder.setTag(0);
			builder.setId(connectionId);
			builder.setDst(msg.dstAddr());
			builder.setPort(msg.dstPort());
			clientContext.writeAndFlush(builder.build());
			builder.clear();

//            log.error("Not implementation");
//            Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4);
//            ctx.writeAndFlush(commandResponse).addListener(ChannelFutureListener.CLOSE);;
		}
	}

	public Socks5CommandRequestHandler(ProxyContextConfiguration proxyContextConfig) {
	    this.proxyContextConfig = proxyContextConfig;
	}

	static class ProxyHandler extends SimpleChannelInboundHandler<ByteBuf> {
		private String connectionId;
		private ChannelHandlerContext clientContext;
		private ProxyContextConfiguration proxyContextConfig;
		DeviceProxyMessageProto.DeviceProxyMessage.Builder builder = DeviceProxyMessageProto.DeviceProxyMessage.newBuilder();


		protected AtomicLong consumeMsgLength;
		protected Runnable counterTask;
		protected final int M = 1024 * 1024;
		protected final int KB = 1024;


		public ProxyHandler(ProxyContextConfiguration proxyContextConfig, String connectionId, ChannelHandlerContext clientContext) {
			this.connectionId = connectionId;
			this.clientContext = clientContext;
			this.proxyContextConfig = proxyContextConfig;
        }

/*		@Override
		public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
			consumeMsgLength = new AtomicLong();
			counterTask = () -> {
				while (true) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {

					}
					long length = consumeMsgLength.getAndSet(0);
					log.info("Socks5CommandRequestHandler************* rate（KB/S）：" + (length / KB));
				}
			};
		}*/

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
	    	log.info("start PROXY_DATA connection id {}", connectionId);
	        int readLength = Math.min(msg.readableBytes(), MAX_DATA_LEN);
	        while (readLength > 0) {
                byte[] data = new byte[readLength];
                msg.readBytes(data);
//                ProxyDataMessage proxyDataMessage = ProxyDataMessage.builder().id(connectionId).data(data).build();
//				clientContext.writeAndFlush(DeviceProxyMessageFactory.encodeProxyDataMessage(proxyDataMessage));

				builder.setTag(0);
				builder.setData(ByteString.copyFrom(data));
				builder.setCode(MessageCode.PROXY_DATA.value());
				builder.setId(connectionId);
				clientContext.writeAndFlush(builder.build());
				builder.clear();

                readLength = Math.min(msg.readableBytes(), MAX_DATA_LEN);
            }
			log.info("end PROXY_DATA connection id {}", connectionId);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			super.channelInactive(ctx);
//            DisconnectRequest disconnectRequest = new DisconnectRequest();
//            disconnectRequest.setId(connectionId);
//            clientContext.writeAndFlush(DeviceProxyMessageFactory.encodeProxyMessage(MessageCode.DISCONNECT, disconnectRequest));

			builder.setTag(0);
//			builder.setData(ByteString.copyFrom(disconnectRequest.));
			builder.setCode(MessageCode.DISCONNECT.value());
			builder.setId(connectionId);
			clientContext.writeAndFlush(builder.build());
			builder.clear();

			this.proxyContextConfig.removeSocksContext(connectionId);
			log.info("close a connection id {}", connectionId);
        }
    }


}
